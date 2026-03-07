package work.socialhub.kgrpc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MethodDescriptorTest {

    @Test
    fun testMethodTypeValues() {
        val types = MethodDescriptor.MethodType.entries
        assertEquals(4, types.size)
        assertEquals(MethodDescriptor.MethodType.UNARY, types[0])
        assertEquals(MethodDescriptor.MethodType.SERVER_STREAMING, types[1])
        assertEquals(MethodDescriptor.MethodType.CLIENT_STREAMING, types[2])
        assertEquals(MethodDescriptor.MethodType.BIDI_STREAMING, types[3])
    }

    @Test
    fun testDataClassEquality() {
        val desc1 = MethodDescriptor(
            fullMethodName = "/test.Service/Method",
            methodType = MethodDescriptor.MethodType.UNARY
        )
        val desc2 = MethodDescriptor(
            fullMethodName = "/test.Service/Method",
            methodType = MethodDescriptor.MethodType.UNARY
        )

        assertEquals(desc1, desc2)
        assertEquals(desc1.hashCode(), desc2.hashCode())
    }

    @Test
    fun testDataClassInequality() {
        val unary = MethodDescriptor(
            fullMethodName = "/test.Service/Method",
            methodType = MethodDescriptor.MethodType.UNARY
        )
        val streaming = MethodDescriptor(
            fullMethodName = "/test.Service/Method",
            methodType = MethodDescriptor.MethodType.SERVER_STREAMING
        )

        assertNotEquals(unary, streaming)
    }

    @Test
    fun testFullMethodName() {
        val desc = MethodDescriptor(
            fullMethodName = "/grpc.health.v1.Health/Check",
            methodType = MethodDescriptor.MethodType.UNARY
        )
        assertEquals("/grpc.health.v1.Health/Check", desc.fullMethodName)
    }
}
