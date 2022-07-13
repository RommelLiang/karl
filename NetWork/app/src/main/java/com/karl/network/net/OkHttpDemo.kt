package com.karl.network.net

import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException


class OkHttpDemo {


}


fun okHttpCall(name: String) {
    //创建OkHttpClient对象
    val client = OkHttpClient()
        .newBuilder()
        .addInterceptor(HttpLoggingInterceptor())
        .build()
    //创建Request请求对象
    var request: Request = Request.Builder()
        .url("http://api.github.com/users/$name/repos")
        .method("GET", null)
        .build()
    //同步请求,获取获取Response对象
    val response = client.newCall(request).execute()
    println(response.body()?.source()?.readUtf8())

    ////异步请求,获取获取Response对象
    /*client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body()?.source()?.readUtf8())
        }
    })*/
}

fun okHttp2() {
    val protocols: MutableList<Protocol> = ArrayList()
    protocols.add(Protocol.H2_PRIOR_KNOWLEDGE)
    protocols.add(Protocol.HTTP_1_1)
    //创建OkHttpClient对象
    val client =
        OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor())
            //.protocols(protocols)
            .build()
    //创建Request请求对象
    val newBuilder = HttpUrl.parse("http://www.bing.com/HPImageArchive.aspx")!!.newBuilder()
    newBuilder.addQueryParameter("format", "js")
    newBuilder.addQueryParameter("idx", "1")
    newBuilder.addQueryParameter("n", "10")
    newBuilder.addQueryParameter("mkt", "en-US")
    var request: Request = Request.Builder()
        .url(newBuilder.build())
        .build()
    val response = client.newCall(request).execute()
    println(response.body()?.source()?.readUtf8())
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















