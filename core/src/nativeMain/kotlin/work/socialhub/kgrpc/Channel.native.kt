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
            // TODO: store interceptors for native RPC calls
        }

        actual fun withKeepAliveConfig(config: KeepAliveConfig): Builder = apply {
            // TODO: pass to Rust/Tonic channel builder
        }

        actual fun withTrustedCertificates(vararg certificates: Certificate): Builder = apply {
            // TODO: pass to Rust/Tonic TLS config
        }

        actual fun withTrustedCertificates(certificates: List<Certificate>): Builder = apply {
            // TODO: pass to Rust/Tonic TLS config
        }

        actual fun trustOnlyProvidedCertificates(): Builder = apply {
            // TODO: pass to Rust/Tonic TLS config
        }

        actual fun withClientIdentity(certificate: Certificate, key: PrivateKey): Builder = apply {
            // TODO: pass to Rust/Tonic TLS config
        }

        actual fun build(): Channel {
            // TODO: create Rust/Tonic channel via cinterop
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
        // TODO: call Rust channel shutdown via cinterop
        terminated = true
    }

    actual suspend fun shutdownNow() {
        // TODO: call Rust channel shutdown via cinterop
        terminated = true
    }
}
