package com.karl.network.net

import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException


class OkHttpDemo {


}


fun okHttpCall(name: String) {
    //创建OkHttpClient对象
    val client = OkHttpClient().newBuilder().addInterceptor(HttpLoggingInterceptor()).build()
    //创建Request请求对象
    var request: Request = Request.Builder()
        .url("https://api.github.com/users/$name/repos")
        .method("GET", null)
        .build()
    //同步请求,获取获取Response对象
    val response = client.newCall(request).execute()
    println(response.body()?.source()?.readUtf8())

    ////异步请求,获取获取Response对象
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body()?.source()?.readUtf8())
        }
    })
}

fun synchronizeCall(name: String) {
    var request: Request = Request.Builder()
        .url("https://api.github.com/users/$name/repos")
        .method("GET", null)
        .build()
    val client = OkHttpClient()
    val response = client.newCall(request).execute()
    println(response.body()?.source()?.readUtf8())
}




