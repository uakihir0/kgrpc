package work.socialhub.kgrpc.testing

/**
 * Minimal protobuf encoding/decoding utilities for integration tests.
 * Supports only the subset needed for addsvc.Add/Sum (int64 and string fields).
 */
internal object ProtobufEncoding {

    fun encodeVarint(value: Long): ByteArray {
        val result = mutableListOf<Byte>()
        var v = value
        while (v and 0x7FL.inv() != 0L) {
            result.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        result.add((v and 0x7F).toByte())
        return result.toByteArray()
    }

    fun decodeVarint(data: ByteArray, offset: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var pos = offset
        while (pos < data.size) {
            val b = data[pos].toLong() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            pos++
            if (b and 0x80 == 0L) break
            shift += 7
        }
        return result to pos
    }

    // Protobuf wire type 0 (varint) tag for a field number
    fun varintTag(fieldNumber: Int): Byte = ((fieldNumber shl 3) or 0).toByte()

    // Protobuf wire type 2 (length-delimited) tag for a field number
    fun lengthDelimitedTag(fieldNumber: Int): Byte = ((fieldNumber shl 3) or 2).toByte()

    fun encodeInt64Field(fieldNumber: Int, value: Long): ByteArray {
        if (value == 0L) return byteArrayOf() // default value, omitted in proto3
        return byteArrayOf(varintTag(fieldNumber)) + encodeVarint(value)
    }

    fun encodeStringField(fieldNumber: Int, value: String): ByteArray {
        if (value.isEmpty()) return byteArrayOf() // default value, omitted in proto3
        val bytes = value.encodeToByteArray()
        return byteArrayOf(lengthDelimitedTag(fieldNumber)) + encodeVarint(bytes.size.toLong()) + bytes
    }

    /**
     * Decode fields from a protobuf message.
     * Returns a map of fieldNumber -> list of values (Long for varint, ByteArray for length-delimited).
     */
    fun decodeFields(data: ByteArray): Map<Int, List<Any>> {
        val fields = mutableMapOf<Int, MutableList<Any>>()
        var pos = 0
        while (pos < data.size) {
            val (tagValue, nextPos) = decodeVarint(data, pos)
            pos = nextPos
            val fieldNumber = (tagValue ushr 3).toInt()
            val wireType = (tagValue and 0x7).toInt()

            when (wireType) {
                0 -> { // varint
                    val (value, np) = decodeVarint(data, pos)
                    pos = np
                    fields.getOrPut(fieldNumber) { mutableListOf() }.add(value)
                }
                2 -> { // length-delimited
                    val (length, np) = decodeVarint(data, pos)
                    pos = np
                    val bytes = data.copyOfRange(pos, pos + length.toInt())
                    pos += length.toInt()
                    fields.getOrPut(fieldNumber) { mutableListOf() }.add(bytes)
                }
                else -> {
                    // skip unknown wire types - for simplicity, break
                    break
                }
            }
        }
        return fields
    }
}
