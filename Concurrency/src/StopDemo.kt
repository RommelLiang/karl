fun main() {
    val car = Car()
    val stopThread = StopThread(car)
    stopThread.start()
    Thread.sleep(500)
    stopThread.stop()
    car.drive()

}

class StopThread(var car: Car) : Thread() {

    override fun run() {
        car.color = "红色"
        sleep(1000)
        car.engine = "V8"
    }
}

class Car {
    var color: String? = null
    var engine: String? = null

    fun drive() {
        println("$color 的车装着$engine 跑起来了")
    }
}