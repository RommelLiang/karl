package performance

class Comp {

    inline fun change(name: String, func: (String) -> Unit) {
        func(name)
    }

    private val valueLazy by lazy {
        "Lazy init"
    }

    fun execute() {
        var i = 2
        change("Lambda"){
            println(it)
            println(i)
        }
    }
}


