package performance



fun main() {
    val comp = Comp()
    comp.numbers(1, 2, 3, 4)

    val intArrayOfOne = intArrayOf(1, 2, 3, 4)
    comp.numbers(*intArrayOfOne)

    val intArrayOfTwo = intArrayOf(1, 2, 3, 4)
    comp.numbers(*intArrayOfTwo, 4, 5, 6)

    var nums = intArrayOf(1, 2, 3, 4)

    KotlinVararg().numbers(*nums)
    nums.forEach {
        print(it)
    }
}

