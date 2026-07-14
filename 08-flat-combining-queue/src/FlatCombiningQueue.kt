import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author Shalanov Dmitriy
 */
class FlatCombiningQueue<E> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    override fun enqueue(element: E) {
        var isTaskSet = false
        var taskIndex: Int = randomCellIndex()
        while (true) {
            if (combinerLock.compareAndSet(false, true)) {
                helpOthers()
                if (isTaskSet) {
                    tasksForCombiner.getAndSet(taskIndex, null)
                } else {
                    queue.addLast(element)
                }
                combinerLock.compareAndSet(true, false)
                break
            }
            if (isTaskSet) {
                val check = tasksForCombiner.get(taskIndex)
                if (check is Result<*>) {
                    tasksForCombiner.set(taskIndex, null)
                    break
                }
            } else {
                if (tasksForCombiner.compareAndSet(taskIndex, null, element)) {
                    isTaskSet = true
                    continue
                }
                taskIndex = randomCellIndex()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        var isTaskSet = false
        var taskIndex: Int = randomCellIndex()
        while (true) {
            if (combinerLock.compareAndSet(false, true)) {
                helpOthers()
                val result = if (isTaskSet) {
                    (tasksForCombiner.getAndSet(taskIndex, null) as Result<*>).value
                } else {
                    queue.removeFirstOrNull()
                }
                combinerLock.compareAndSet(true, false)
                return result as E?
            }
            if (isTaskSet) {
                val check = tasksForCombiner.get(taskIndex)
                if (check is Result<*>) {
                    tasksForCombiner.set(taskIndex, null)
                    return check.value as E?
                }
            } else {
                if (tasksForCombiner.compareAndSet(taskIndex, null, Dequeue)) {
                    isTaskSet = true
                    continue
                }
                taskIndex = randomCellIndex()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun helpOthers() {
        for (i in 0 until tasksForCombiner.length()) {
            when (val task = tasksForCombiner.get(i)) {
                is Result<*>, null -> continue
                is Dequeue -> tasksForCombiner.set(i, Result(queue.removeFirstOrNull()))
                else -> tasksForCombiner.set(i, Result(queue.addLast(task as E)))
            }
        }
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

private object Dequeue

private class Result<V>(
    val value: V
)