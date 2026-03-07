package work.socialhub.kgrpc

import work.socialhub.kgrpc.stub.Stub
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class StubTest {

    private class TestStub(val deadline: Duration? = null) : Stub<TestStub>() {
        override fun withDeadlineAfter(duration: Duration): TestStub {
            return TestStub(deadline = duration)
        }
    }

    @Test
    fun testWithDeadlineAfterReturnsNewStub() {
        val stub = TestStub()
        val withDeadline = stub.withDeadlineAfter(5.seconds)

        assertNotNull(withDeadline)
        assertNotEquals(stub, withDeadline)
        assertEquals(5.seconds, withDeadline.deadline)
    }

    @Test
    fun testOriginalStubUnchanged() {
        val stub = TestStub()
        stub.withDeadlineAfter(10.seconds)

        assertNull(stub.deadline)
    }
}
