package work.socialhub.kgrpc

import work.socialhub.kgrpc.message.Message
import work.socialhub.kgrpc.metadata.Key
import work.socialhub.kgrpc.metadata.Metadata
import kotlin.test.Test
import kotlin.test.assertEquals

class CallInterceptorTest {

    private val testMethod = MethodDescriptor(
        fullMethodName = "/test.Service/Method",
        methodType = MethodDescriptor.MethodType.UNARY
    )

    @Test
    fun testDefaultImplementationsReturnInputsUnchanged() {
        val interceptor = object : CallInterceptor {}
        val metadata = Metadata.of(Key.AsciiKey("key"), "value")
        val status = Status(Code.OK, "")

        assertEquals(metadata, interceptor.onStart(testMethod, metadata))
        assertEquals(metadata, interceptor.onReceiveHeaders(testMethod, metadata))
        assertEquals(status to metadata, interceptor.onClose(testMethod, status, metadata))
    }

    @Test
    fun testOnStartAddsMetadata() {
        val authKey = Key.AsciiKey("authorization")

        val interceptor = object : CallInterceptor {
            override fun onStart(methodDescriptor: MethodDescriptor, metadata: Metadata): Metadata {
                return metadata.withEntry(authKey, "Bearer token123")
            }
        }

        val result = interceptor.onStart(testMethod, Metadata.empty())
        assertEquals("Bearer token123", result[authKey])
    }

    @Test
    fun testOnCloseModifiesStatus() {
        val interceptor = object : CallInterceptor {
            override fun onClose(
                methodDescriptor: MethodDescriptor,
                status: Status,
                trailers: Metadata
            ): Pair<Status, Metadata> {
                return Status(Code.OK, "overridden") to trailers
            }
        }

        val (status, _) = interceptor.onClose(
            testMethod,
            Status(Code.INTERNAL, "error"),
            Metadata.empty()
        )
        assertEquals(Code.OK, status.code)
        assertEquals("overridden", status.statusMessage)
    }
}
