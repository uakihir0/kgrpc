package work.socialhub.kgrpc

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import work.socialhub.kgrpc.config.KeepAliveConfig
import work.socialhub.kgrpc.native.*
import kotlin.time.Duration.Companion.seconds

internal val ENABLE_TRACE_LOGGING = false

actual class Channel private constructor(
    internal val name: String,
    internal val port: Int,
    private val usePlaintextFlag: Boolean,
    private val certificates: List<Certificate>?,
    private val trustOnlyProvidedCertificates: Boolean,
    private val identity: ClientIdentity?,
    internal val interceptors: List<CallInterceptor>,
    internal val keepAliveConfig: KeepAliveConfig
) {

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    internal val context = newSingleThreadContext("kgrpc channel executor - $name:$port")

    internal val channel: CPointer<cnames.structs.RustChannel>?

    private val cleanupMutex = Mutex()
    internal val isShutdownState = MutableStateFlow(false)
    internal val isShutdownImmediately = MutableStateFlow(false)
    private val activeRpcs = MutableStateFlow(0)
    private var hasCleanedUpResources = false

    init {
        @Suppress("HttpUrlsUsage")
        val host = (if (usePlaintextFlag) "http://" else "https://") + "$name:$port"

        val enableKeepalive = when (keepAliveConfig) {
            is KeepAliveConfig.Enabled -> true
            KeepAliveConfig.Disabled -> false
        }

        val (keepAliveTime, keepAliveTimeout, keepAliveWithoutCalls) = when (keepAliveConfig) {
            is KeepAliveConfig.Disabled -> Triple(0.seconds, 0.seconds, false)
            is KeepAliveConfig.Enabled -> Triple(
                keepAliveConfig.time,
                keepAliveConfig.timeout,
                keepAliveConfig.withoutCalls
            )
        }

        val builder = channel_builder_create(
            host = host,
            enable_keepalive = enableKeepalive,
            keepalive_time_nanos = keepAliveTime.inWholeNanoseconds.toULong(),
            keepalive_timeout_nanos = keepAliveTimeout.inWholeNanoseconds.toULong(),
            keepalive_without_calls = keepAliveWithoutCalls
        ) ?: throw IllegalArgumentException("$host is not a valid uri.")

        if (!usePlaintextFlag) {
            val tlsConfig = tls_config_create()
            if (certificates != null) {
                installCertificates(certificates, tlsConfig)
            }

            if (!trustOnlyProvidedCertificates) {
                tls_config_use_webpki_roots(tlsConfig)
            }

            if (identity != null) {
                identity.key.asPem.encodeToByteArray().toUByteArray().usePinned { key ->
                    identity.cert.asPem.encodeToByteArray().toUByteArray().usePinned { cert ->
                        val success = tls_config_use_client_credentials(
                            config = tlsConfig,
                            key_data = key.addressOf(0),
                            key_len = key.get().size.toULong(),
                            cert_data = cert.addressOf(0),
                            cert_len = cert.get().size.toULong()
                        )

                        if (!success) {
                            throw IllegalArgumentException("Failed to configure client identity. Invalid certificate or private key.")
                        }
                    }
                }
            }

            channel_builder_use_tls_config(builder, tlsConfig)
        }

        channel = channel_builder_build(builder)

        if (channel == null) {
            throw IllegalArgumentException("$host is not a valid uri.")
        }
    }

    actual class Builder(private val name: String, private val port: Int) {

        private var usePlaintextFlag = false
        private var certificates: List<Certificate>? = null
        private var trustOnlyProvidedCerts = false
        private var identity: ClientIdentity? = null
        private val interceptors = mutableListOf<CallInterceptor>()
        private var keepAliveConfig: KeepAliveConfig = KeepAliveConfig.Disabled

        actual companion object {
            actual fun forAddress(name: String, port: Int): Builder = Builder(name, port)
        }

        actual fun usePlaintext(): Builder = apply {
            usePlaintextFlag = true
            certificates = null
        }

        actual fun withInterceptors(vararg interceptors: CallInterceptor): Builder = apply {
            this.interceptors += interceptors.toList()
        }

        actual fun withKeepAliveConfig(config: KeepAliveConfig): Builder = apply {
            keepAliveConfig = config
        }

        actual fun withTrustedCertificates(vararg certificates: Certificate): Builder =
            withTrustedCertificates(certificates.toList())

        actual fun withTrustedCertificates(certificates: List<Certificate>): Builder = apply {
            usePlaintextFlag = false
            this.certificates = certificates
        }

        actual fun trustOnlyProvidedCertificates(): Builder = apply {
            trustOnlyProvidedCerts = true
        }

        actual fun withClientIdentity(certificate: Certificate, key: PrivateKey): Builder = apply {
            identity = ClientIdentity(key, certificate)
        }

        actual fun build(): Channel {
            return Channel(
                name = name,
                port = port,
                usePlaintextFlag = usePlaintextFlag,
                certificates = certificates,
                trustOnlyProvidedCertificates = trustOnlyProvidedCerts,
                identity = identity,
                interceptors = interceptors.toList(),
                keepAliveConfig = keepAliveConfig
            )
        }
    }

    actual val isTerminated: Boolean
        get() = isShutdownState.value && activeRpcs.value == 0 && hasCleanedUpResources

    actual suspend fun shutdown() {
        isShutdownState.value = true

        cleanupMutex.withLock {
            activeRpcs.first { it == 0 }

            if (hasCleanedUpResources) return@withLock

            channel_free(channel)
            context.close()
            hasCleanedUpResources = true
        }
    }

    actual suspend fun shutdownNow() {
        isShutdownImmediately.value = true
        shutdown()
    }

    internal fun registerRpc() {
        activeRpcs.update { it + 1 }
    }

    internal fun unregisterRpc() {
        activeRpcs.update { it - 1 }
    }

    companion object {
        init {
            init(ENABLE_TRACE_LOGGING)
        }
    }
}

private fun installCertificates(
    certificates: List<Certificate>,
    tlsConfig: CPointer<cnames.structs.RustTlsConfigBuilder>?
) {
    certificates.forEachIndexed { index, certificate ->
        if (certificate.data.isEmpty()) {
            throw IllegalArgumentException("Certificate data is empty at index $index")
        }

        certificate.data.toUByteArray().usePinned { pinned ->
            val isSuccessful = tls_config_install_certificate(
                tlsConfig,
                pinned.addressOf(0),
                pinned.get().size.toULong()
            )

            if (!isSuccessful) {
                throw IllegalArgumentException("Failed to install certificate at index $index. Invalid TLS certificate format.")
            }
        }
    }
}

private data class ClientIdentity(val key: PrivateKey, val cert: Certificate)
