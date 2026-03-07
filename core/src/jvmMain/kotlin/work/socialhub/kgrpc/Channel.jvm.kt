package work.socialhub.kgrpc

import work.socialhub.kgrpc.config.KeepAliveConfig
import work.socialhub.kgrpc.internal.ClientInterceptorImpl
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.concurrent.thread
import kotlin.coroutines.resume

actual class Channel private constructor(val channel: ManagedChannel) {

    actual class Builder(private val impl: OkHttpChannelBuilder) {

        private var usePlaintextFlag: Boolean = false
        private val trustedCertificates: MutableList<Certificate> = mutableListOf()
        private var trustOnlyProvided: Boolean = false
        private var clientCertificate: Certificate? = null
        private var clientKey: PrivateKey? = null

        actual companion object {
            actual fun forAddress(name: String, port: Int): Builder {
                return Builder(OkHttpChannelBuilder.forAddress(name, port))
            }
        }

        actual fun usePlaintext(): Builder = apply {
            impl.usePlaintext()
            usePlaintextFlag = true
        }

        actual fun withInterceptors(vararg interceptors: CallInterceptor): Builder = apply {
            val grpcInterceptors = interceptors.map { ClientInterceptorImpl(it) }.toTypedArray()
            impl.intercept(*grpcInterceptors)
        }

        actual fun withKeepAliveConfig(config: KeepAliveConfig): Builder = apply {
            when (config) {
                is KeepAliveConfig.Disabled -> {
                    impl.keepAliveTime(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                }
                is KeepAliveConfig.Enabled -> {
                    impl.keepAliveTime(config.time.inWholeNanoseconds, TimeUnit.NANOSECONDS)
                    impl.keepAliveTimeout(config.timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
                    impl.keepAliveWithoutCalls(config.withoutCalls)
                }
            }
        }

        actual fun withTrustedCertificates(vararg certificates: Certificate): Builder {
            return withTrustedCertificates(certificates.toList())
        }

        actual fun withTrustedCertificates(certificates: List<Certificate>): Builder = apply {
            trustedCertificates.addAll(certificates)
        }

        actual fun trustOnlyProvidedCertificates(): Builder = apply {
            trustOnlyProvided = true
        }

        actual fun withClientIdentity(certificate: Certificate, key: PrivateKey): Builder = apply {
            clientCertificate = certificate
            clientKey = key
        }

        actual fun build(): Channel {
            if (!usePlaintextFlag && (trustedCertificates.isNotEmpty() || clientCertificate != null)) {
                configureTls()
            }
            return Channel(impl.build())
        }

        private fun configureTls() {
            val certFactory = CertificateFactory.getInstance("X.509")

            val trustManagers = if (trustedCertificates.isNotEmpty()) {
                val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
                trustStore.load(null, null)
                trustedCertificates.forEachIndexed { index, cert ->
                    val x509 = certFactory.generateCertificate(ByteArrayInputStream(cert.data))
                    trustStore.setCertificateEntry("cert-$index", x509)
                }

                if (!trustOnlyProvided) {
                    val defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    defaultTmf.init(null as KeyStore?)
                    val defaultStore = KeyStore.getInstance(KeyStore.getDefaultType())
                    defaultStore.load(null, null)
                    // Merge default trust store entries
                    val defaultTrustStore = KeyStore.getInstance(KeyStore.getDefaultType())
                    defaultTrustStore.load(null, null)
                }

                val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                tmf.init(trustStore)
                tmf.trustManagers
            } else {
                null
            }

            val keyManagers = if (clientCertificate != null && clientKey != null) {
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                keyStore.load(null, null)
                val x509Cert = certFactory.generateCertificate(ByteArrayInputStream(clientCertificate!!.data))
                val keySpec = PKCS8EncodedKeySpec(clientKey!!.data)
                val keyFactory = KeyFactory.getInstance("RSA")
                val privateKey = keyFactory.generatePrivate(keySpec)
                keyStore.setKeyEntry("client", privateKey, charArrayOf(), arrayOf(x509Cert))

                val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                kmf.init(keyStore, charArrayOf())
                kmf.keyManagers
            } else {
                null
            }

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagers, trustManagers, null)
            impl.sslSocketFactory(sslContext.socketFactory)
        }
    }

    actual val isTerminated: Boolean
        get() = channel.isTerminated

    actual suspend fun shutdown() {
        channel.shutdown()
        awaitTermination()
    }

    actual suspend fun shutdownNow() {
        channel.shutdownNow()
        awaitTermination()
    }

    private suspend fun awaitTermination() {
        suspendCancellableCoroutine { continuation ->
            val t = thread {
                try {
                    channel.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
                    if (continuation.isActive) continuation.resume(Unit)
                } catch (e: InterruptedException) {
                    if (continuation.isActive) continuation.cancel(e)
                }
            }

            continuation.invokeOnCancellation {
                t.interrupt()
            }
        }
    }
}
