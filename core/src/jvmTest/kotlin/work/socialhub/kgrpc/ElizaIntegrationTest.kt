package work.socialhub.kgrpc

import io.grpc.CallOptions
import io.grpc.MethodDescriptor as JvmMethodDescriptor
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import work.socialhub.kgrpc.metadata.Metadata
import work.socialhub.kgrpc.rpc.serverStreamingRpc
import work.socialhub.kgrpc.rpc.unaryRpc
import work.socialhub.kgrpc.testing.IntroduceRequest
import work.socialhub.kgrpc.testing.IntroduceResponse
import work.socialhub.kgrpc.testing.SayRequest
import work.socialhub.kgrpc.testing.SayResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test using the public demo.connectrpc.com ElizaService (TLS on port 443).
 * This server provides a gRPC-compatible Eliza chatbot.
 */
class ElizaIntegrationTest {

    companion object {
        private const val HOST = "demo.connectrpc.com"
        private const val PORT = 443
    }

    /**
     * Test unary RPC: ElizaService/Say
     * Sends a sentence and expects a non-empty response.
     */
    @Test
    fun testElizaSay() = runTest(timeout = 15.seconds) {
        val channel = Channel.Builder
            .forAddress(HOST, PORT)
            .build()

        try {
            val method = JvmMethodDescriptor.newBuilder<SayRequest, SayResponse>()
                .setType(JvmMethodDescriptor.MethodType.UNARY)
                .setFullMethodName("connectrpc.eliza.v1.ElizaService/Say")
                .setRequestMarshaller(messageMarshaller(SayRequest.Companion))
                .setResponseMarshaller(messageMarshaller(SayResponse.Companion))
                .build()

            val request = SayRequest(sentence = "Hello, I am testing kgrpc!")
            val response = unaryRpc(
                channel = channel,
                method = method,
                request = request,
                callOptions = CallOptions.DEFAULT,
                headers = Metadata.empty()
            )

            assertNotNull(response)
            assertTrue(response.sentence.isNotEmpty(), "Expected non-empty response from Eliza")
            println("Eliza Say response: ${response.sentence}")
        } finally {
            channel.shutdownNow()
        }
    }

    /**
     * Test server streaming RPC: ElizaService/Introduce
     * Sends a name and expects multiple introduction sentences streamed back.
     */
    @Test
    fun testElizaIntroduce() = runTest(timeout = 15.seconds) {
        val channel = Channel.Builder
            .forAddress(HOST, PORT)
            .build()

        try {
            val method = JvmMethodDescriptor.newBuilder<IntroduceRequest, IntroduceResponse>()
                .setType(JvmMethodDescriptor.MethodType.SERVER_STREAMING)
                .setFullMethodName("connectrpc.eliza.v1.ElizaService/Introduce")
                .setRequestMarshaller(messageMarshaller(IntroduceRequest.Companion))
                .setResponseMarshaller(messageMarshaller(IntroduceResponse.Companion))
                .build()

            val request = IntroduceRequest(name = "kgrpc")
            val responses = serverStreamingRpc(
                channel = channel,
                method = method,
                request = request,
                callOptions = CallOptions.DEFAULT,
                headers = Metadata.empty()
            ).toList()

            assertTrue(responses.isNotEmpty(), "Expected at least one streaming response")
            responses.forEach { response ->
                assertTrue(response.sentence.isNotEmpty(), "Each response should have a sentence")
                println("Eliza Introduce response: ${response.sentence}")
            }
        } finally {
            channel.shutdownNow()
        }
    }

    /**
     * Creates a grpc-java Marshaller that serializes/deserializes using our Message interface.
     */
    private fun <T : work.socialhub.kgrpc.message.Message> messageMarshaller(
        companion: work.socialhub.kgrpc.message.MessageCompanion<T>
    ): io.grpc.MethodDescriptor.Marshaller<T> {
        return object : io.grpc.MethodDescriptor.Marshaller<T> {
            override fun stream(value: T): InputStream {
                return ByteArrayInputStream(value.serialize())
            }

            override fun parse(stream: InputStream): T {
                return companion.deserialize(stream.readBytes())
            }
        }
    }
}
