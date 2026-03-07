package work.socialhub.kgrpc

data class MethodDescriptor(
    val fullMethodName: String,
    val methodType: MethodType
) {
    enum class MethodType {
        UNARY,
        SERVER_STREAMING,
        CLIENT_STREAMING,
        BIDI_STREAMING
    }
}
