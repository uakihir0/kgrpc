package work.socialhub.kgrpc

object KGrpcFactory {

    fun channel(
        host: String,
        port: Int,
    ) = Channel.Builder
        .forAddress(host, port)
}
