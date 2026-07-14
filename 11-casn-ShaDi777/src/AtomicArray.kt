import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.concurrent.atomic.AtomicReference

class AtomicArray<E>(size: Int, initialValue: E) {
    private val a = atomicArrayOfNulls<Any>(size)

    init {
        for (i in 0 until size) a[i].value = initialValue
    }

    fun get(index: Int): E? {
        while (true) {
            when (val it = a[index].value) {
                is Descriptor -> it.complete()
                else -> return it as E?
            }
        }
    }

    fun set(index: Int, value: Any?) {
        a[index].value = value
    }

    fun cas(index: Int, expected: Any?, update: Any?): Boolean {
        while (true) {
            if (get(index) != expected) return false
            if (a[index].compareAndSet(expected, update)) return true
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        if (index1 == index2) {
            return expected1 == expected2 && cas(index1, expected1, update2)
        }

        val (triple1, triple2) = if (index1 < index2) {
            listOf(
                Triple(index1, expected1, update1),
                Triple(index2, expected2, update2),
            )
        } else {
            listOf(
                Triple(index2, expected2, update2),
                Triple(index1, expected1, update1),
            )
        }

        val descriptor = CasnDescriptor(
            triple1.first, triple1.second, triple1.third,
            triple2.first, triple2.second, triple2.third,
        )

        if (!cas(triple1.first, triple1.second, descriptor)) return false

        descriptor.complete()
        return descriptor.outcome.get() == Outcome.SUCCESS
    }

    fun dcss(
        index1: Int, expected1: Any?, update1: Any?,
        element2: AtomicReference<Outcome>, expected2: Any?,
    ): Boolean {
        val descriptor = DcssDescriptor(
            index1, expected1, update1,
            element2, expected2
        )

        if (!cas(index1, expected1, descriptor)) return false

        descriptor.complete()
        return descriptor.outcome.get() == Outcome.SUCCESS
    }

    inner class DcssDescriptor(
        val index1: Int,
        val expected1: Any?,
        val update1: Any?,
        val element2: AtomicReference<Outcome>,
        val expected2: Any?,
        var outcome: AtomicReference<Outcome> = AtomicReference(Outcome.UNDECIDED),
    ) : Descriptor() {
        override fun complete() {
            outcome.compareAndSet(
                Outcome.UNDECIDED,
                if (element2.get() == expected2) Outcome.SUCCESS else Outcome.FAILURE
            )
            if (outcome.get() == Outcome.SUCCESS) {
                a[index1].compareAndSet(this, update1)
            } else {
                a[index1].compareAndSet(this, expected1)
            }
        }
    }

    inner class CasnDescriptor(
        val index1: Int,
        val expected1: Any?,
        val update1: Any?,
        val index2: Int,
        val expected2: Any?,
        val update2: Any?,
        val outcome: AtomicReference<Outcome> = AtomicReference(Outcome.UNDECIDED),
    ) : Descriptor() {
        override fun complete() {
            val descriptorReady = a[index2].value == this || dcss(index2, expected2, this, outcome, Outcome.UNDECIDED)
            val finalOutcome = if (descriptorReady) Outcome.SUCCESS else Outcome.FAILURE
            outcome.compareAndSet(Outcome.UNDECIDED, finalOutcome)
            if (outcome.get() == Outcome.SUCCESS) {
                a[index1].compareAndSet(this, update1)
                a[index2].compareAndSet(this, update2)
            } else {
                a[index1].compareAndSet(this, expected1)
                a[index2].compareAndSet(this, expected2)
            }
        }
    }
}

abstract class Descriptor() {
    abstract fun complete()
}

enum class Outcome {
    UNDECIDED, SUCCESS, FAILURE
}
