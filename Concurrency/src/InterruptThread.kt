import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue

fun main() {
    val queue = SynchronousQueue<Int>()
    val interruptThread = InterruptThread(queue).apply { start() }
    interruptThread.takeValue()
}

class InterruptThread(private val queue: BlockingQueue<Int>) : Thread() {
    private var i = 1
    private var j = 1

    override fun run() {
        println("斐波那契数列开始")
        print("数字：")
        try {
            while (!isInterrupted) {
                j += i
                i = j - i
                queue.put(i)
            }
        } catch (e: InterruptedException) {
            println("中断了")
        }
        println("斐波那契数列结束")
    }

    fun takeValue() {
        sleep(10000)
        while (!this.isInterrupted) {
            var num = queue.take()
            if (num > 200) {
                println()
                cancel()
            } else {
                print("$num ")
                sleep(500)
            }
        }
    }

    private fun cancel() {
        interrupt()
    }
}