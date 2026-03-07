package work.socialhub.kgrpc

import io.ktor.client.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import work.socialhub.kgrpc.config.KeepAliveConfig

actual class Channel private constructor(
    internal val host: String,
    internal val port: Int,
    internal val usePlaintextFlag: Boolean,
    internal val interceptors: List<CallInterceptor>
) {

    @Suppress("HttpUrlsUsage")
    internal val connectionString = (if (usePlaintextFlag) "http://" else "https://") + "$host:$port"

    internal val client = HttpClient {
        install(HttpTimeout) {}
    }

    private val cleanupMutex = Mutex()
    internal val isShutdownState = MutableStateFlow(false)
    internal val isShutdownImmediately = MutableStateFlow(false)
    private val activeRpcs = MutableStateFlow(0)
    private var hasCleanedUpResources = false

    actual class Builder(private val name: String, private val port: Int) {

        private var usePlaintextFlag = false
        private val interceptors = mutableListOf<CallInterceptor>()

        actual companion object {
            actual fun forAddress(name: String, port: Int): Builder = Builder(name, port)
        }

        actual fun usePlaintext(): Builder = apply {
            usePlaintextFlag = true
        }

        actual fun withInterceptors(vararg interceptors: CallInterceptor): Builder = apply {
            this.interceptors += interceptors.toList()
        }

        actual fun withKeepAliveConfig(config: KeepAliveConfig): Builder = apply {
            // no-op on JS
        }

        actual fun withTrustedCertificates(vararg certificates: Certificate): Builder = apply {
            // no-op on JS (browser-level security)
        }

        actual fun withTrustedCertificates(certificates: List<Certificate>): Builder = apply {
            // no-op on JS
        }

        actual fun trustOnlyProvidedCertificates(): Builder = apply {
            // no-op on JS
        }

        actual fun withClientIdentity(certificate: Certificate, key: PrivateKey): Builder = apply {
            // no-op on JS
        }

        actual fun build(): Channel {
            return Channel(
                host = name,
                port = port,
                usePlaintextFlag = usePlaintextFlag,
                interceptors = interceptors.toList()
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

            client.close()
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
}
