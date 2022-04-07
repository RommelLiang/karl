package com.karl.network

import com.karl.network.bean.Bean
import com.karl.network.bean.Repo
import com.karl.network.net.GitHubService
import org.junit.Test

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val call = GitHubService.service.listRepos("RommelLiang")
        println("${Thread.currentThread().name}")
        call.enqueue(object : Callback<Bean?> {
            override fun onResponse(
                call: Call<Bean?>,
                response: Response<Bean?>
            ) {
                println("${Thread.currentThread().name}")
                response.body().toString()
            }

            override fun onFailure(call: Call<Bean?>, t: Throwable) {
                println(t.message)
            }
        })
    }
}