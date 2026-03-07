package work.socialhub.kgrpc.internal

import work.socialhub.kgrpc.CallInterceptor
import work.socialhub.kgrpc.Code
import work.socialhub.kgrpc.Status
import work.socialhub.kgrpc.message.Message
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import io.grpc.Metadata as JvmMetadata
import io.grpc.MethodDescriptor as JvmMethodDescriptor
import io.grpc.Status as JvmStatus

internal class ClientInterceptorImpl(private val impl: CallInterceptor) : ClientInterceptor {

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: JvmMethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val methodDescriptor = work.socialhub.kgrpc.MethodDescriptor(
            fullMethodName = method.fullMethodName,
            methodType = when (method.type) {
                JvmMethodDescriptor.MethodType.UNARY -> work.socialhub.kgrpc.MethodDescriptor.MethodType.UNARY
                JvmMethodDescriptor.MethodType.CLIENT_STREAMING -> work.socialhub.kgrpc.MethodDescriptor.MethodType.CLIENT_STREAMING
                JvmMethodDescriptor.MethodType.SERVER_STREAMING -> work.socialhub.kgrpc.MethodDescriptor.MethodType.SERVER_STREAMING
                JvmMethodDescriptor.MethodType.BIDI_STREAMING, JvmMethodDescriptor.MethodType.UNKNOWN, null -> work.socialhub.kgrpc.MethodDescriptor.MethodType.BIDI_STREAMING
            }
        )

        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: JvmMetadata) {
                val newMetadata = impl.onStart(
                    methodDescriptor = methodDescriptor,
                    metadata = headers.kmMetadata
                )

                super.start(
                    object : SimpleForwardingClientCallListener<RespT>(responseListener) {
                        override fun onHeaders(headers: JvmMetadata) {
                            super.onHeaders(impl.onReceiveHeaders(methodDescriptor, headers.kmMetadata).jvmMetadata)
                        }

                        @Suppress("UNCHECKED_CAST")
                        override fun onMessage(message: RespT) {
                            if (message is Message) {
                                super.onMessage(impl.onReceiveMessage(methodDescriptor, message) as RespT)
                            } else {
                                super.onMessage(message)
                            }
                        }

                        override fun onClose(status: JvmStatus, trailers: JvmMetadata) {
                            val (newStatus, newTrailers) = impl.onClose(
                                methodDescriptor = methodDescriptor,
                                status = Status(
                                    code = Code.getCodeForValue(status.code.value()),
                                    statusMessage = status.description.orEmpty()
                                ),
                                trailers = trailers.kmMetadata
                            )

                            super.onClose(
                                JvmStatus.fromCodeValue(newStatus.code.value).withDescription(newStatus.statusMessage),
                                newTrailers.jvmMetadata
                            )
                        }
                    },
                    newMetadata.jvmMetadata
                )
            }

            @Suppress("UNCHECKED_CAST")
            override fun sendMessage(message: ReqT) {
                if (message is Message) {
                    super.sendMessage(impl.onSendMessage(methodDescriptor, message) as ReqT)
                } else {
                    super.sendMessage(message)
                }
            }
        }
    }
}
