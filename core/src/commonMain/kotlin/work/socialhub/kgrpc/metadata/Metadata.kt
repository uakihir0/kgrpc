package work.socialhub.kgrpc.metadata

class Metadata private constructor(
    internal val asciiMap: MultiMap<Key.AsciiKey, String>,
    internal val binaryMap: MultiMap<Key.BinaryKey, ByteArray>
) {

    private constructor(
        asciiMap: Map<Key.AsciiKey, Set<String>>,
        binaryMap: Map<Key.BinaryKey, Set<ByteArray>>
    ) : this(MultiMap(asciiMap), MultiMap(binaryMap))

    companion object {
        fun of(vararg entries: Entry<*>): Metadata = of(entries.toList())

        @Suppress("UNCHECKED_CAST")
        fun of(entries: List<Entry<*>>): Metadata {
            val asciiMap = mutableMapOf<Key.AsciiKey, Set<String>>()
            val binaryMap = mutableMapOf<Key.BinaryKey, Set<ByteArray>>()

            entries.groupBy { it.key }.forEach { (key, entries) ->
                when (key) {
                    is Key.AsciiKey -> {
                        asciiMap[key] = entries.flatMap { it.values }.toSet() as Set<String>
                    }
                    is Key.BinaryKey -> {
                        binaryMap[key] = entries.flatMap { it.values }.toSet() as Set<ByteArray>
                    }
                }
            }

            return Metadata(MultiMap(asciiMap), MultiMap(binaryMap))
        }

        fun <T> of(key: Key<T>, value: T): Metadata {
            return when (key) {
                is Key.AsciiKey -> Metadata(mapOf(key to setOf(value as String)), emptyMap())
                is Key.BinaryKey -> Metadata(emptyMap(), mapOf(key to setOf(value as ByteArray)))
            }
        }

        fun empty(): Metadata = Metadata(emptyMap(), emptyMap())
    }

    val keys: Set<Key<*>> = asciiMap.keys + binaryMap.keys

    val entries: List<Entry<*>>
        get() = asciiMap.entries.map { Entry.Ascii(it.first, it.second) } +
                binaryMap.entries.map { Entry.Binary(it.first, it.second) }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Key<T>): T? = when (key) {
        is Key.AsciiKey -> asciiMap.getLast(key) as T?
        is Key.BinaryKey -> binaryMap.getLast(key) as T?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getAll(key: Key<T>): Set<T> = when (key) {
        is Key.AsciiKey -> asciiMap.getAll(key) as Set<T>
        is Key.BinaryKey -> binaryMap.getAll(key) as Set<T>
    }

    operator fun plus(other: Metadata): Metadata =
        Metadata(asciiMap + other.asciiMap, binaryMap + other.binaryMap)

    operator fun <T> minus(key: Key<T>): Metadata = when (key) {
        is Key.AsciiKey -> Metadata(asciiMap - key, binaryMap)
        is Key.BinaryKey -> Metadata(asciiMap, binaryMap - key)
    }

    fun <T> withEntry(key: Key<T>, value: T): Metadata = when (key) {
        is Key.AsciiKey -> Metadata(asciiMap + (key to value as String), binaryMap)
        is Key.BinaryKey -> Metadata(asciiMap, binaryMap + (key to value as ByteArray))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Metadata
        return asciiMap == other.asciiMap && binaryMap == other.binaryMap
    }

    override fun hashCode(): Int {
        var result = asciiMap.hashCode()
        result = 31 * result + binaryMap.hashCode()
        return result
    }

    override fun toString(): String = "Metadata(entries=$entries)"
}
