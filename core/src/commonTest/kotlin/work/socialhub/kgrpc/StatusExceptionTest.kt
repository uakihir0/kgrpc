package work.socialhub.kgrpc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class StatusExceptionTest {

    @Test
    fun testUnavailableDueToShutdown() {
        val ex = StatusException.UnavailableDueToShutdown
        assertEquals(Code.UNAVAILABLE, ex.status.code)
        assertNull(ex.cause)
    }

    @Test
    fun testCancelledDueToShutdown() {
        val ex = StatusException.CancelledDueToShutdown
        assertEquals(Code.CANCELLED, ex.status.code)
    }

    @Test
    fun testInternalOnlyExpectedOneElement() {
        val ex = StatusException.InternalOnlyExpectedOneElement
        assertEquals(Code.INTERNAL, ex.status.code)
    }

    @Test
    fun testInternalExpectedAtLeastOneElement() {
        val ex = StatusException.InternalExpectedAtLeastOneElement
        assertEquals(Code.INTERNAL, ex.status.code)
    }

    @Test
    fun testRequestTimeout() {
        val duration = 5.seconds
        val ex = StatusException.requestTimeout(duration, null)
        assertEquals(Code.DEADLINE_EXCEEDED, ex.status.code)
        assert(ex.status.statusMessage.contains("5s"))
    }

    @Test
    fun testInternalWithMessage() {
        val ex = StatusException.internal("something went wrong")
        assertEquals(Code.INTERNAL, ex.status.code)
        assertEquals("something went wrong", ex.status.statusMessage)
    }

    @Test
    fun testMessageReturnsStatusString() {
        val ex = StatusException(
            status = Status(code = Code.NOT_FOUND, statusMessage = "resource not found"),
            cause = null
        )
        assertEquals("Status(code=NOT_FOUND, statusMessage=resource not found)", ex.message)
    }
}
