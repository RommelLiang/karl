package com.karl.network.vue

import android.webkit.JavascriptInterface

class JsInterface {

    @JavascriptInterface
    fun fromVue(name:String){
        println("-------$name")
    }
}