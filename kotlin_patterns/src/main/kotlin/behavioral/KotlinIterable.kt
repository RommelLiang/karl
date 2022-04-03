package behavioral

fun main() {
    val delegateUserList = DelegateUserList(arrayListOf("K", "A", "R", "L"))
    val operatorUserList = OperatorUserList(arrayListOf("K", "A", "R", "L"))
    val mutableIteratorUserList = MutableIteratorUserList(arrayListOf("K", "A", "R", "L"))

    println("委托机制：")
    for (name in delegateUserList) {
        print(name)
    }
    println("\n运算符重载机制：")
    for (name in operatorUserList) {
        print(name)
    }

    println("\nMutableIterator：")
    for (name in mutableIteratorUserList) {
        print(name)
        if (name == "K") mutableIteratorUserList.remove()
    }

    mutableIteratorUserList.printlnElement()


}

/**
 * 委托机制实现
 * */
class DelegateUserList(private val names: List<String>) : Iterator<String> by names.iterator()

/**
* 运算符重载
* */
class OperatorUserList(private val names: List<String>) {
    operator fun iterator(): Iterator<String> = names.iterator()
}

class MutableIteratorUserList(private val names: List<String>):MutableIterator<String> by names.iterator() as MutableIterator<String> {
    fun printlnElement(){
        println("\n$names")
    }
}