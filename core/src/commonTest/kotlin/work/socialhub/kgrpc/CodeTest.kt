package work.socialhub.kgrpc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CodeTest {

    @Test
    fun testGetCodeForValue() {
        assertEquals(Code.OK, Code.getCodeForValue(0))
        assertEquals(Code.CANCELLED, Code.getCodeForValue(1))
        assertEquals(Code.UNAVAILABLE, Code.getCodeForValue(14))
        assertEquals(Code.UNAUTHENTICATED, Code.getCodeForValue(16))
    }

    @Test
    fun testGetCodeForInvalidValue() {
        assertFailsWith<IllegalArgumentException> {
            Code.getCodeForValue(99)
        }
    }
}
