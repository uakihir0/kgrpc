package work.socialhub.kgrpc

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ChannelBuilderTest {

    @Test
    fun testCreatePlaintextChannel() = runTest {
        val channel = Channel.Builder
            .forAddress("localhost", 50051)
            .usePlaintext()
            .build()

        assertNotNull(channel)
        assertFalse(channel.isTerminated)

        channel.shutdownNow()
    }

    @Test
    fun testChannelShutdown() = runTest {
        val channel = Channel.Builder
            .forAddress("localhost", 50051)
            .usePlaintext()
            .build()

        assertFalse(channel.isTerminated)
        channel.shutdownNow()
        assert(channel.isTerminated)
    }
}
