package com.karl.network.vue

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONArray

class JsInterface(private var context: Context?) {

    private var webView: WebView? = null
    private var register: Intent? = null
    private val wifiManager: WifiManager by lazy {
        context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    public fun setWeb(webView:WebView){
        this.webView = webView
    }
    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                var i = 0
                val list  = arrayListOf<Map<String,String>>()
                wifiManager.scanResults.forEach {
                    val json = "{'token':'2','id':'${it.SSID}','name':'${it.SSID}'}"

                    webView?.evaluateJavascript("javascript:wifiFunction(\"$json\")"){
                        Log.e("$json",it)
                    }

                }
                val name = JSONArray(list).toString()


            } else {

            }
        }
    }

    @JavascriptInterface
    fun fromVue(name: String) {
        println("-------$name")
    }
    private val mHandler = object :Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val name = "asd"

        }
    }
    @JavascriptInterface
    fun scanWifi() {
        register?.apply {
            context?.unregisterReceiver(wifiScanReceiver)
        }
        mHandler.obtainMessage().apply {
            mHandler.sendMessage(this)
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        val registerReceiver = context?.registerReceiver(wifiScanReceiver, intentFilter)
        val success = wifiManager.startScan()
    }


    fun clear() {
        this.context = null
    }


    @JavascriptInterface
    fun getLocalIp() {
        Log.e("xcs","-------------")
        val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress

        webView?.evaluateJavascript("javascript:linkNativeCallBack(\"$ipAddress\")"){}
    }

    @JavascriptInterface
    fun postMessage(params:String){
        Log.e("xcd",params)
    }
}