package work.socialhub.kgrpc

import work.socialhub.kgrpc.stub.Stub
import kotlin.test.Test
import kotlin.test.assertNotNull
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
        assert(withDeadline !== stub)
        assert(withDeadline.deadline == 5.seconds)
    }

    @Test
    fun testOriginalStubUnchanged() {
        val stub = TestStub()
        stub.withDeadlineAfter(10.seconds)

        assert(stub.deadline == null)
    }
}
