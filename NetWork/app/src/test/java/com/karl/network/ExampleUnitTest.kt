package com.karl.network


import com.karl.network.bean.Repo
import com.karl.network.net.GitHubService
import com.karl.network.net.okHttpCall
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.junit.Test
import java.io.IOException
import java.util.*


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun test() {
        okHttpCall("RommelLiang")
        Thread.sleep(20000)
        val listRepos = GitHubService.service.listRepos("RommelLiang")

    }

}





