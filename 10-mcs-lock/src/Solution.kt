import java.util.concurrent.atomic.AtomicReference

/**
 * @author Shalanov Dmitriy
 */
class Solution(val env: Environment) : Lock<Solution.Node> {
    private val tail = AtomicReference<Node?>(null)

    override fun lock(): Node {
        val my = Node()

        val prev = tail.getAndSet(my)
        if (prev != null) {
            prev.next.value = my
            while (my.locked.value) env.park()
        }

        return my
    }

    override fun unlock(my: Node) {
        if (my.next.value == null) {
            if (tail.compareAndSet(my, null)) return
            while (my.next.value == null) continue
        }

        my.next.value?.locked?.value = false
        env.unpark(my.next.value!!.thread)
    }

    class Node {
        val thread = Thread.currentThread()
        val next = AtomicReference<Node?>(null)
        val locked = AtomicReference(true)
    }
}
