package work.socialhub.kgrpc.metadata

sealed interface Entry<T> {
    val key: Key<T>
    val values: Set<T>

    data class Ascii(override val key: Key.AsciiKey, override val values: Set<String>) : Entry<String>
    data class Binary(override val key: Key.BinaryKey, override val values: Set<ByteArray>) : Entry<ByteArray>
}
