package behavioral


fun main() {
    GoToWork(::proletariat).go()
    GoToWork(::middleClass).go()
    GoToWork(::capitalist).go()
}

class GoToWork(val way: () -> Unit) {
    fun go() {
        way()
    }
}

fun proletariat() {
    println("无产阶级只能骑自行车")
}

fun middleClass() {
    println("中产可以开小汽车")
}

fun capitalist() {
    println("资本家不需要上班，因为他们挂在路灯上")
}