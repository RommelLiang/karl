package com.karl.network


import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import java.util.*


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    var head: Node? = null
    var last: Node? = null

    @Test
    fun test() {
        val pq = PriorityQueue<Int>()
        pq.offer(1)
    }

}

class Node(val name: String) {

    var next: Node? = null

    override fun toString(): String {
        return "${name}:{${next}}"
    }
}



