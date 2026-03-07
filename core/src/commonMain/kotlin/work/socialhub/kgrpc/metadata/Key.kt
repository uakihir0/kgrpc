package work.socialhub.kgrpc.metadata

sealed interface Key<T> {

    companion object {
        internal const val BINARY_KEY_SUFFIX = "-bin"

        fun fromName(name: String): Key<*> {
            return if (name.endsWith(BINARY_KEY_SUFFIX)) {
                BinaryKey(name)
            } else {
                AsciiKey(name)
            }
        }
    }

    val name: String

    data class AsciiKey(override val name: String) : Key<String> {
        init {
            if (name.endsWith(BINARY_KEY_SUFFIX)) {
                throw IllegalArgumentException("name must not end with $BINARY_KEY_SUFFIX")
            }
        }
    }

    data class BinaryKey(override val name: String) : Key<ByteArray> {
        init {
            if (!name.endsWith(BINARY_KEY_SUFFIX)) {
                throw IllegalArgumentException("name must end with $BINARY_KEY_SUFFIX")
            }
        }
    }
}
