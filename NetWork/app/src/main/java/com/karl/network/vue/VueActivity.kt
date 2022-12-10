package com.karl.network.vue

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.webkit.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.karl.network.R
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import java.lang.Exception
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit


class VueActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var button: Button
    private val jsInterface: JsInterface by lazy {
        JsInterface(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.game_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toJs -> {

                webView.evaluateJavascript("javascript:callJs()") {
                    println("-----$it")
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vue)
        initServer()
        initWeb()
    }

    private fun initServer() {

        Thread {
            val server: Server = AndServer.webServer(this)
                .port(8080)
                .timeout(10, TimeUnit.SECONDS)
                .inetAddress(InetAddress.getLocalHost())
                .listener(object : Server.ServerListener {
                    override fun onStarted() {
                        runOnUiThread {
                            webView.loadUrl("http://localhost:8080")
                        }
                    }

                    override fun onStopped() {
                        Log.e("--", "onStopped")
                    }

                    override fun onException(e: Exception?) {
                        Log.e("--", "err")
                    }

                })
                .build()
            server.startup()
        }.start()


    }

    override fun onDestroy() {
        jsInterface.clear()
        super.onDestroy()
    }

    private fun initWeb() {

        WebView.setWebContentsDebuggingEnabled(true)
        webView = findViewById<WebView>(R.id.web_view).apply {
            clearHistory()
            with(settings) {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                databaseEnabled = true
                allowFileAccessFromFileURLs = true
                domStorageEnabled = true
                allowFileAccess = true
                allowUniversalAccessFromFileURLs = true

            }
            addJavascriptInterface(jsInterface, "LinkNative")
            jsInterface.setWeb(this)
            webViewClient = Client()
            webChromeClient = ChromeClient()
        }
        button = findViewById(R.id.button)
        button.setOnClickListener {
            webView.loadUrl("javascript:callJS()");
        }
    }

    inner class ChromeClient : WebChromeClient() {

        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            println("----------$message")
            println("-------------${Uri.parse(message).scheme}")
            val isInterface = Uri.parse(message).scheme.equals("js")
            if (isInterface) {
                result?.confirm()
                return true
            }
            return super.onJsAlert(view, url, message, result)
        }

        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            println("----------$message")
            println("-------------${Uri.parse(message).scheme}")
            val isInterface = Uri.parse(url).scheme.equals("js")
            if (isInterface) {
                println(Uri.parse(url))
                result?.confirm("ohhhhh")
                return true
            }
            return super.onJsPrompt(view, url, message, defaultValue, result)
        }

        override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            val isInterface = Uri.parse(url).scheme.equals("js")
            if (isInterface) {
                println(Uri.parse(url))
                return true
            }
            return super.onJsConfirm(view, url, message, result)
        }

    }

    inner class Client : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            Log.e("--------onPageStarted:$url", "")
            super.onPageFinished(view, url)

        }

        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {

            return super.shouldInterceptRequest(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            println("--------onPageStarted:$url")
            super.onPageStarted(view, url, favicon)
        }


        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request?.url?.path == "/") {
                view?.loadUrl("file:///android_asset/xcs-app-web/index.html")
                return true
            }
            val isInterface = request?.url?.let {
                println("---------${it.scheme}")
                println("---------${it.host}")

                it.scheme.equals("js")
            }

            val isFile = request?.url?.let {

                it.scheme.equals("file")
            }


            if (isInterface == true) {
                return isInterface
            } else if (isFile == true) {
                return true;
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {

            error.apply {
                println("--------Error:${error?.errorCode}")
                println("--------Error:${error?.description}")
            }
            super.onReceivedError(view, request, error)
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
        }

    }


    fun onServerStart(ip: String) {
        webView.loadUrl(ip)
    }

    override fun onStop() {
        super.onStop()
    }
}
