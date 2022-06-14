package performance

class Comp constructor() {

    private val valueLazy by lazy(LazyThreadSafetyMode.NONE) {
        "Lazy init"
    }

    /*private inline fun change(name: String, func: (String) -> Unit) {
        func(name)
    }*/

    private fun change(name: String, func: (String) -> Unit) {
        func(name)
    }

    fun funcCapturing(value: String) {
        println(value)
    }

    fun execute() {
        change("funcCapturing", ::funcCapturing)
    }

    fun executeCapturing() {
        var i = 2
        change("捕获式Lambda") {
            println(it)
            println(i)
        }
    }

    fun executeNonCapturing() {
        change("非捕获式Lambda表达式") {
            var i = 2
            println(it)
            println(i)
        }
    }

    fun numbers(vararg nums: Int) {
        nums.forEach {
            println(it)
        }
    }

    fun out() {
        var a = 1

        fun innerOne(i: Int) = i + 1
        fun innerTwo(i: Int) = i + a

        val innerOneNum = innerOne(2)
        println(innerOneNum)

        val innerTwoNum = innerTwo(2)
        println(innerTwoNum)
    }

    fun loopMethod() {
        for (i in 1..100) {
            println(1)
        }
    }

    fun arrayOfFunc() {
        val intOne = arrayOf(1, 2, 3)
        val intTwo = intArrayOf(1, 2, 3)

        val doubleOne = arrayOf(1.0, 2.0, 3.0)
        val doubleTwo = doubleArrayOf(1.0, 2.0, 3.0)

        val longOne = arrayOf(1L, 2L, 3L)
        val longTwo = longArrayOf(1, 2, 3)

        val charOne = arrayOf('a', 'b', 'c')
        val charTwo = charArrayOf('a', 'b', 'c')

        val arrayInteger = Array(3) { 1;2;3 }
        val arrayInt = IntArray(3) { 1;2;3 }
    }


}





