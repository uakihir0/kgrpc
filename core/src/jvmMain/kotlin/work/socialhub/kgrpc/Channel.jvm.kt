package work.socialhub.kgrpc

import work.socialhub.kgrpc.config.KeepAliveConfig
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.resume

actual class Channel private constructor(val channel: ManagedChannel) {

    actual class Builder(private val impl: OkHttpChannelBuilder) {

        private var usePlaintextFlag: Boolean = false

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
            // TODO: wrap CallInterceptor to io.grpc.ClientInterceptor
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
            // TODO: install certificates into SSL context
        }

        actual fun trustOnlyProvidedCertificates(): Builder = apply {
            // TODO: disable default trust store
        }

        actual fun withClientIdentity(certificate: Certificate, key: PrivateKey): Builder = apply {
            // TODO: configure mTLS
        }

        actual fun build(): Channel {
            return Channel(impl.build())
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
