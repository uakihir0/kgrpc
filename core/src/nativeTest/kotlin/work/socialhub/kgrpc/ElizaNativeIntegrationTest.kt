package work.socialhub.kgrpc

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import work.socialhub.kgrpc.metadata.Metadata
import work.socialhub.kgrpc.rpc.serverStreamingRpc
import work.socialhub.kgrpc.rpc.unaryRpc
import work.socialhub.kgrpc.testing.IntroduceRequest
import work.socialhub.kgrpc.testing.IntroduceResponse
import work.socialhub.kgrpc.testing.SayRequest
import work.socialhub.kgrpc.testing.SayResponse
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Native integration test using the public demo.connectrpc.com ElizaService (TLS on port 443).
 */
class ElizaNativeIntegrationTest {

    companion object {
        private const val HOST = "demo.connectrpc.com"
        private const val PORT = 443
    }

    /**
     * Test unary RPC: ElizaService/Say via Rust/Tonic FFI.
     */
    @Test
    fun testElizaSay() = runTest(timeout = 15.seconds) {
        val channel = Channel.Builder
            .forAddress(HOST, PORT)
            .build()

        try {
            val request = SayRequest(sentence = "Hello from kgrpc native!")
            val response = unaryRpc(
                channel = channel,
                path = "/connectrpc.eliza.v1.ElizaService/Say",
                request = request,
                responseDeserializer = SayResponse.Companion,
                headers = Metadata.empty()
            )

            assertNotNull(response)
            assertTrue(response.sentence.isNotEmpty(), "Expected non-empty response from Eliza")
            println("Native Eliza Say response: ${response.sentence}")
        } finally {
            channel.shutdownNow()
        }
    }

    /**
     * Test server streaming RPC: ElizaService/Introduce via Rust/Tonic FFI.
     */
    @Test
    fun testElizaIntroduce() = runTest(timeout = 15.seconds) {
        val channel = Channel.Builder
            .forAddress(HOST, PORT)
            .build()

        try {
            val request = IntroduceRequest(name = "kgrpc")
            val responses = serverStreamingRpc(
                channel = channel,
                path = "/connectrpc.eliza.v1.ElizaService/Introduce",
                request = request,
                responseDeserializer = IntroduceResponse.Companion,
                headers = Metadata.empty()
            ).toList()

            assertTrue(responses.isNotEmpty(), "Expected at least one streaming response")
            responses.forEach { response ->
                assertTrue(response.sentence.isNotEmpty(), "Each response should have a sentence")
                println("Native Eliza Introduce response: ${response.sentence}")
            }
        } finally {
            channel.shutdownNow()
        }
    }
}
