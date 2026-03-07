package work.socialhub.kgrpc

import work.socialhub.kgrpc.config.KeepAliveConfig

actual class Channel private constructor(
    internal val host: String,
    internal val port: Int,
    internal val usePlaintextFlag: Boolean
) {

    actual class Builder(private val name: String, private val port: Int) {

        private var usePlaintextFlag = false

        actual companion object {
            actual fun forAddress(name: String, port: Int): Builder = Builder(name, port)
        }

        actual fun usePlaintext(): Builder = apply {
            usePlaintextFlag = true
        }

        actual fun withInterceptors(vararg interceptors: CallInterceptor): Builder = apply {
            // TODO: store interceptors
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
                usePlaintextFlag = usePlaintextFlag
            )
        }
    }

    @Volatile
    private var terminated = false

    actual val isTerminated: Boolean
        get() = terminated

    actual suspend fun shutdown() {
        terminated = true
    }

    actual suspend fun shutdownNow() {
        terminated = true
    }
}
