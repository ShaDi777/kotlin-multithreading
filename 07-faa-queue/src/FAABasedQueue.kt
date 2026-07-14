import java.util.concurrent.atomic.*

/**
 * @author Shalanov Dmitriy
 */
class FAABasedQueue<E> : Queue<E> {
    private val head: AtomicReference<Segment>
    private val tail: AtomicReference<Segment>
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    init {
        val init = AtomicReference(Segment(0))
        head = init
        tail = init
    }

    override fun enqueue(element: E) {
        while (true) {
            val curTail = tail.get()
            val i = enqIdx.getAndIncrement()
            val segment = findSegment(start = curTail, id = i / SEGMENT_SIZE)
            tail.compareAndSet(curTail.next.get(), segment)
            if (segment.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (!shouldTryToDequeue()) return null
            val curHead = head.get()
            val i = deqIdx.getAndIncrement()
            val segment = findSegment(start = curHead, id = i / SEGMENT_SIZE)
            head.compareAndSet(curHead.next.get(), segment)
            if (segment.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, POISONED)) continue
            return segment.cells.getAndSet((i % SEGMENT_SIZE).toInt(), null) as E
        }
    }

    private fun shouldTryToDequeue(): Boolean {
        while (true) {
            val curEnqIdx = enqIdx.get()
            val curDeqIdx = deqIdx.get()
            if (curEnqIdx != enqIdx.get()) continue
            return curDeqIdx < curEnqIdx
        }
    }

    private fun findSegment(start: Segment, id: Long): Segment {
        var curStart = start
        while(id > curStart.id) {
            val next = curStart.next.get() ?: Segment(id = curStart.id + 1)
            curStart.next.compareAndSet(null, next)
            curStart = curStart.next.get()!!
        }
        return curStart
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2
private val POISONED = Any()
