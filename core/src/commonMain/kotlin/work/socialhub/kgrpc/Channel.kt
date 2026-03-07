package work.socialhub.kgrpc

import work.socialhub.kgrpc.config.KeepAliveConfig

expect class Channel {
    class Builder {
        companion object {
            fun forAddress(name: String, port: Int): Builder
        }

        fun usePlaintext(): Builder

        fun withInterceptors(vararg interceptors: CallInterceptor): Builder

        fun withKeepAliveConfig(config: KeepAliveConfig): Builder

        fun withTrustedCertificates(vararg certificates: Certificate): Builder

        fun withTrustedCertificates(certificates: List<Certificate>): Builder

        fun trustOnlyProvidedCertificates(): Builder

        fun withClientIdentity(certificate: Certificate, key: PrivateKey): Builder

        fun build(): Channel
    }

    val isTerminated: Boolean

    suspend fun shutdown()

    suspend fun shutdownNow()
}
