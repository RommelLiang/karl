package com.karl.network


import com.karl.network.bean.Repo
import com.karl.network.flow.FlowDemo
import com.karl.network.flow.Num
import com.karl.network.net.GitHubService
import com.karl.network.net.okHttpCall
import com.karl.network.view_model.MainViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.*


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @get:Rule

    private lateinit var viewModel: MainViewModel


    @Test
    fun testViewModel() {

        runBlocking {

            /*flow.collect {
                println("-------------------$it")
            }


            flow.collect {
                println("::-------------------$it")
            }*/

            val flowDemo = FlowDemo()

            val mutableStateFlow = flowDemo.mutableSharedFlow

            GlobalScope.launch {
                mutableStateFlow.collect {

                }
            }

            GlobalScope.launch {
                flowDemo.changeShare()
                flowDemo.changeShare()
                flowDemo.changeShare()
                flowDemo.changeShare()
                delay(2000)
                mutableStateFlow.collect {
                    println("!!：$it")
                }
            }
            GlobalScope.launch {
                delay(2000)
                mutableStateFlow.collect {
                    println("。。。。：$it")
                }
            }
            delay(10000)
        }
    }

    @Test
    fun test() {

        /*okHttpCall("RommelLiang")
        Thread.sleep(20000)
        val listRepos = GitHubService.service.listRepos("RommelLiang")*/
        /*val flowDemo = FlowDemo()

        val mutableStateFlow = flowDemo.mutableStateFlow
        GlobalScope.launch {
            println("-------${Thread.currentThread().name}")
            mutableStateFlow.collect(::afterEmit)
        }

        println("-------${mutableStateFlow.value}")

        *//* flowDemo.changeValue()
         flowDemo.changeValue()*//*
        runBlocking {
            repeat(2) {
                flowDemo.changeValue()
                delay(1000)
            }
            delay(5000)
            flowDemo.changeValue()
            delay(100000)

        }*/
        val a = A()
        runBlocking(Dispatchers.IO) {

            /* flow { repeat(3) {emit(it) } }

             flow(::doubleJump)
              flow{
                  val asFlow = asFlow()
                  emit(asFlow)
              }.collect {
                  println("-----asFlow----$it")
              }*/
            flowOf(
                flow {
                    emit(1)
                    println("${Thread.currentThread().name}-----list----1")
                    delay(1000)
                    emit(2)
                },
                flow {
                    emit(3)
                },
                flow {
                    emit(4)
                },
                flow {
                    emit(5)
                },
            ).flowOn(Dispatchers.IO).flattenMerge().collect {
                println("${Thread.currentThread().name}-----list----$it")
            }
        }
    }

    private suspend fun afterEmit(it: Num) {
        println("1:::::${it.count}")
    }

    private suspend fun asFlow(): Int {
        delay(1000)
        return 8
    }

    private suspend fun doubleJump(flos: FlowCollector<Int>) {
        repeat(3) {
            println("发送数据了啊")
            flos.emit(it)
        }
    }


    inner class A : FlowCollector<Int> {
        override suspend fun emit(value: Int) {
            println("---")
        }

    }

    @Test
    fun testFlow() {

        val flowDemo = FlowDemo()

        val mutableStateFlow = flowDemo.mutableSharedFlow

        runBlocking {

        }

    }

    @Test
    fun testApi() {
        var start = System.currentTimeMillis()
        val list = mutableListOf<Deferred<Int>>()
        val oddNumbers = sequence {
            yield(1)
            yieldAll(listOf(3, 5))
            yieldAll(generateSequence(7) { it + 2 })
        }
        println(oddNumbers.take(5).toList())
        runBlocking {
            flow {
                repeat(30) {
                    list.add(async { delayTimes(it) })
                }
                list.forEach {
                    val await = it.await()
                    emit(await)
                }
            }.flowOn(Dispatchers.IO).collect {
                delay(300)
                println(it)
            }

        }
        GlobalScope.launch(Dispatchers.IO) { }

        println(System.currentTimeMillis() - start)
    }

    suspend fun delayTimes(i: Int): Int {
        delay(100)
        return i

    }

    @Test
    fun test2() {
        val aa = Aa()
        val demo = flow {
            repeat(20) {
                emit(aa.getData())
            }

        }.flowOn(Dispatchers.Default)

        GlobalScope.launch(Dispatchers.IO) {
            demo.collect {
                println("1::$it")
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            demo.collect {
                println("2::$it")
            }
        }
        sequence {
            yield(1)
        }
        Thread.sleep(3000)
    }

    internal class Aa {
        private val mutex = Mutex()
        var i = 0
        suspend fun getData(): Int {
            mutex.withLock {
                delay(10)
                return i++
            }
        }
    }

    @Test
    fun runFun() {
        flowDemo()
    }
}


fun flowDemo() {
    val numbers = flow {
        println("this is:$this and hashcode is ${this.hashCode()}")
        emit(1)
    }.flowOn(Dispatchers.IO)

    GlobalScope.launch(Dispatchers.IO) {
        numbers.collect {
            println("第一个：numbers is:$numbers and hashcode is ${numbers.hashCode()}")
        }
    }.cancel()
    GlobalScope.launch(Dispatchers.IO) {
        numbers.collect {
            println("第二个：numbers is:$numbers and hashcode is ${numbers.hashCode()}")
        }
    }

    Thread.sleep(10009)
}


var i = 1
val mutex = Mutex()
suspend fun getNumber(): Int {
    mutex.withLock {
        delay(10)
        return i++
    }
}






