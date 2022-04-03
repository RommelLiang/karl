package behavioral


class KotlinTemplate {

    fun func1() = println("第一个")
    fun func2() = println("第二个")
    fun func3() = println("第三个")
}

fun initData(first: () -> Unit, second: () -> Unit, third: () -> Unit){
    first()
    second()
    third()
}

fun main() {
    val template = KotlinTemplate()
    initData(template::func1, template::func2, template::func3)
}