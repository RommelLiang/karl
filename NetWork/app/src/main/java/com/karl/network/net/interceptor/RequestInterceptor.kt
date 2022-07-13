package com.karl.network.net.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.e("RequestInterceptor","-----------${Thread.currentThread().name}")
        Log.e("Request-Method","-----------${request.method()}")
        Log.e("Request-Host","-----------${request.url()}")
        for (headName in request.headers().names()){
            Log.e("Request-Head:$headName","-----------${request.header(headName)}")
        }
        Log.e("Request-Body","-----------${request.body()}")
        return chain.proceed(request)
    }
}