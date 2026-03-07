package work.socialhub.kgrpc.metadata

internal data class MultiMap<K, V>(
    private val impl: Map<K, Set<V>> = emptyMap()
) {
    val keys: Set<K> get() = impl.keys

    val entries: List<Pair<K, Set<V>>> get() = impl.entries.map { (key, values) -> Pair(key, values) }

    fun getLast(key: K): V? = impl[key]?.lastOrNull()

    fun getAll(key: K): Set<V> = impl[key].orEmpty()

    operator fun plus(entry: Pair<K, V>): MultiMap<K, V> {
        val newValue = impl.getOrElse(entry.first) { emptySet() } + entry.second
        return MultiMap(impl + Pair(entry.first, newValue))
    }

    operator fun plus(other: MultiMap<K, V>): MultiMap<K, V> {
        val newMap = impl.toMutableMap()
        other.impl.forEach { (key, values) ->
            val existingValues = newMap.getOrElse(key) { emptySet() }
            newMap[key] = existingValues + values
        }
        return MultiMap(newMap)
    }

    operator fun minus(key: K): MultiMap<K, V> {
        return MultiMap(impl - key)
    }
}
