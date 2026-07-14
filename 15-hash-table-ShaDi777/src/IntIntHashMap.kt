import kotlinx.atomicfu.AtomicIntArray
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic

/**
 * Int-to-Int hash map with open addressing and linear probes.
 */
class IntIntHashMap {
    private val coreRef = atomic(Core(INITIAL_CAPACITY))
    private val core: Core
        get() = coreRef.value

    /**
     * Returns value for the corresponding key or zero if this key is not present.
     *
     * @param key a positive key.
     * @return value for the corresponding or zero if this key is not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    operator fun get(key: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        return toValue(core.getInternal(key))
    }

    /**
     * Changes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key   a positive key.
     * @param value a positive value.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key or value are not positive, or value is equal to
     * [Integer.MAX_VALUE] which is reserved.
     */
    fun put(key: Int, value: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        require(isValue(value)) { "Invalid value: $value" }
        return toValue(putAndRehashWhileNeeded(key, value))
    }

    /**
     * Removes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key a positive key.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    fun remove(key: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        return toValue(putAndRehashWhileNeeded(key, DEL_VALUE))
    }

    private fun putAndRehashWhileNeeded(key: Int, value: Int): Int {
        while (true) {
            val currentCore = core
            val oldValue = currentCore.putInternal(key, value)
            if (oldValue != NEEDS_REHASH) return oldValue
            coreRef.compareAndSet(currentCore, currentCore.rehash())
        }
    }

    private class Core(capacity: Int) {
        val next: AtomicRef<Core?> = atomic(null)

        // Pairs of <key, value> here, the actual
        // size of the map is twice as big.
        val map: AtomicIntArray = AtomicIntArray(2 * capacity)
        val shift: Int

        init {
            val mask = capacity - 1
            assert(mask > 0 && mask and capacity == 0) { "Capacity must be power of 2: $capacity" }
            shift = 32 - Integer.bitCount(mask)
        }

        fun getInternal(key: Int): Int {
            var index = index(key)
            var probes = 0
            while (true) { // optimize for successful lookup
                val currentKey = map[index].value

                if (currentKey == key) break

                if (currentKey == NULL_KEY) return NULL_VALUE // not found -- no value

                if (++probes >= MAX_PROBES) return NULL_VALUE

                if (index == 0) index = map.size

                index -= 2
            }

            return when (val value = map[index + 1].value) {
                DEL_VALUE -> NULL_VALUE
                COPIED_VALUE -> next.value!!.getInternal(key)
                else -> if (isFixed(value)) -value else value
            }
        }

        fun putInternal(key: Int, value: Int): Int {
            var index = index(key)
            var probes = 0
            while (true) { // optimize for successful lookup
                val currentKey = map[index].value
                val currentValue = map[index + 1].value
                val nextCore = next.value

                if (currentKey == NULL_KEY) {
                    // not found -- claim this slot
                    if (value == DEL_VALUE) return NULL_VALUE // remove of missing item, no need to claim slot
                    if (map[index].compareAndSet(NULL_KEY, key)) {
                        if (map[index + 1].compareAndSet(currentValue, value)) {
                            return currentValue
                        }
                    }
                    continue
                }

                if (currentKey == key) {
                    if (currentValue == COPIED_VALUE) {
                        return nextCore!!.putInternal(key, value)
                    }

                    if (isFixed(currentValue)) {
                        val positiveValue = -currentValue
                        nextCore!!.helpCopy(map[index].value, positiveValue)
                        return nextCore.putInternal(key, value).also {
                            map[index + 1].compareAndSet(currentValue, COPIED_VALUE)
                        }
                    }

                    if (currentValue in NULL_VALUE..DEL_VALUE) {
                        if (!map[index + 1].compareAndSet(currentValue, value)) continue
                        return currentValue
                    }
                }

                if (++probes >= MAX_PROBES) return NEEDS_REHASH
                if (index == 0) index = map.size
                index -= 2
            }
        }

        fun rehash(): Core {
            next.compareAndSet(null, Core(map.size))
            val newCore = next.value!!
            var index = 0
            while (index < map.size) {

                // copy value into new core
                while (true) {
                    val currentValue = map[index + 1].value

                    if (currentValue == COPIED_VALUE) break

                    if (isFixed(currentValue)) {
                        val positiveValue = -currentValue
                        newCore.helpCopy(map[index].value, positiveValue)
                        if (map[index + 1].compareAndSet(currentValue, COPIED_VALUE)) break
                    }

                    if (currentValue in NULL_VALUE..DEL_VALUE) {
                        if (!map[index + 1].compareAndSet(currentValue, -currentValue)) continue
                        newCore.helpCopy(map[index].value, currentValue)
                        if (map[index + 1].compareAndSet(-currentValue, COPIED_VALUE)) break
                    }
                }

                index += 2
            }

            return newCore
        }

        private fun helpCopy(key: Int, value: Int) {
            var index = index(key)
            var probes = 0
            while (true) {
                val currentKey = map[index].value

                if (currentKey == NULL_KEY) {
                    if (map[index].compareAndSet(NULL_KEY, key))
                        if (map[index + 1].compareAndSet(NULL_VALUE, value))
                            return
                    continue
                }
                if (currentKey == key) {
                    map[index + 1].compareAndSet(NULL_VALUE, value)
                    return
                }

                if (++probes >= MAX_PROBES) return
                if (index == 0) index = map.size
                index -= 2
            }
        }

        /**
         * Returns an initial index in map to look for a given key.
         */
        fun index(key: Int): Int = (key * MAGIC ushr shift) * 2
    }
}

private const val MAGIC = -0x61c88647 // golden ratio
private const val INITIAL_CAPACITY = 2 // !!! DO NOT CHANGE INITIAL CAPACITY !!!
private const val MAX_PROBES = 8 // max number of probes to find an item
private const val NULL_KEY = 0 // missing key (initial value)
private const val NULL_VALUE = 0 // missing value (initial value)
private const val DEL_VALUE = Int.MAX_VALUE // mark for removed value
private const val COPIED_VALUE = Int.MIN_VALUE // mark for transferred value
private const val NEEDS_REHASH = -1 // returned by `putInternal` to indicate that rehash is needed

// Checks is the value is in the range of allowed values
private fun isValue(value: Int): Boolean = value in (1 until DEL_VALUE)

// Checks is the value is in the range of allowed values
private fun isFixed(value: Int): Boolean = value in (-Int.MAX_VALUE..-1)

// Converts internal value to the public results of the methods
private fun toValue(value: Int): Int = if (isValue(value)) value else 0