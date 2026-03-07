package work.socialhub.kgrpc

import work.socialhub.kgrpc.metadata.Key
import work.socialhub.kgrpc.metadata.Metadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataTest {

    @Test
    fun testEmptyMetadata() {
        val metadata = Metadata.empty()
        assertEquals(0, metadata.keys.size)
    }

    @Test
    fun testAsciiEntry() {
        val key = Key.AsciiKey("authorization")
        val metadata = Metadata.of(key, "Bearer token123")

        assertEquals("Bearer token123", metadata[key])
    }

    @Test
    fun testMergeMetadata() {
        val key1 = Key.AsciiKey("key1")
        val key2 = Key.AsciiKey("key2")
        val m1 = Metadata.of(key1, "value1")
        val m2 = Metadata.of(key2, "value2")

        val merged = m1 + m2
        assertEquals("value1", merged[key1])
        assertEquals("value2", merged[key2])
    }

    @Test
    fun testRemoveKey() {
        val key1 = Key.AsciiKey("key1")
        val key2 = Key.AsciiKey("key2")
        val metadata = Metadata.of(key1, "value1").withEntry(key2, "value2")

        val result = metadata - key1
        assertNull(result[key1])
        assertEquals("value2", result[key2])
    }
}
