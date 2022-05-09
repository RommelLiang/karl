import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue

fun main() {
    val queue = SynchronousQueue<Int>()
    val blockingThread = BlockingThread(queue).apply { start() }
    blockingThread.takeValue()
}
class BlockingThread(private val queue: BlockingQueue<Int>) : Thread() {
    private var i = 1
    private var j = 1

    @Volatile
    var isCancel = false
    override fun run() {
        println("斐波那契数列开始")
        print("数字：")
        while (!isCancel) {
            j += i
            i = j - i
            queue.put(i)
        }
        println("斐波那契数列结束")
    }

    fun takeValue() {
        var num = 1
        while (num < 200) {
            num = queue.take()
            print("$num ")
            sleep(500)
        }
        cancel()
    }

    private fun cancel() {
        isCancel = true
        println("\n取消了吗？")
    }
}