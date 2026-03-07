package work.socialhub.kgrpc

import work.socialhub.kgrpc.message.Message
import work.socialhub.kgrpc.metadata.Metadata

interface CallInterceptor {

    fun onStart(methodDescriptor: MethodDescriptor, metadata: Metadata): Metadata = metadata

    fun <T : Message> onSendMessage(methodDescriptor: MethodDescriptor, message: T): T = message

    fun onReceiveHeaders(methodDescriptor: MethodDescriptor, metadata: Metadata): Metadata = metadata

    fun <T : Message> onReceiveMessage(methodDescriptor: MethodDescriptor, message: T): T = message

    fun onClose(
        methodDescriptor: MethodDescriptor,
        status: Status,
        trailers: Metadata
    ): Pair<Status, Metadata> = status to trailers
}
