import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.Delayed
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TimeUnit

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val methodType = MethodType.methodType(String::class.java)

        val l = MethodHandles.privateLookupIn(MyString::class.java,MethodHandles.lookup())
        val findVirtual = l.findVirtual(MyString::class.java, "toString", methodType)
        val itemA  = l.findVarHandle(
            MyString::class.java, "a",
            Int::class.java
        )
        val itemB  = l.findVarHandle(
            MyString::class.java, "future",
            Long::class.java
        )
        val myString = MyString(2,"a")
        println(findVirtual.invoke(myString))
        var num = 2
        myString.setNum()

        while (!itemA.compareAndSet(myString, num, 16)){
            println("----------${myString.getNum()}")
            num++
        }
        println("!---------${myString.getNum()}")
        itemA.set(myString,90)
        itemB.set(myString,9000L)
        println("!---------${myString.toString()}")
    }
}

class MyString(private var a: Int,private var b: String) : Delayed {
    private val delay = 1000
    private var future: Long = 0
    init {
        future = System.nanoTime() + delay

    }

    override fun compareTo(other: Delayed?): Int {
        if (other is MyString) return other.a.compareTo(this.a)
        throw IllegalArgumentException()
    }

    override fun getDelay(unit: TimeUnit): Long {
        return future - System.nanoTime()
    }

    fun getNum()  = a

    fun setNum(){
        a = 8
    }

    override fun toString(): String {
        return "$a::::::$future"
    }

}