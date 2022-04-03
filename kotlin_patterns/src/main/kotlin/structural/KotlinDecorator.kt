package structural

fun main() {
    Car().phevRefuel()
    Car().refuel()

}

interface Oil{
    fun refuel()
}

class Car:Oil{
    override fun refuel() {
        println("98加满")
    }

}

fun Car.phevRefuel(){
    this.refuel()
    println("开始充电")
}

/**
 * 同名扩展函数*/
fun Car.refuel(){
    println("我不会被执行，因为永远会以成员函数优先")
}