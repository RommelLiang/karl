import java.lang.Thread.sleep

fun main() {
    val cancelThread = CancelThread().apply { start() }
    sleep(1000)
    cancelThread.cancel()
}

class CancelThread : Thread() {
    private var i = 1
    private var j = 1

    @Volatile
    var isCancel = false
    override fun run() {
        println("斐波那契数列开始\n数字:")
        while (!isCancel) {
            j += i
            i = j - i
            print("$i ")
            sleep(100)
        }
        println("\n斐波那契数列结束")
    }

    fun cancel() {
        isCancel = true
    }
}