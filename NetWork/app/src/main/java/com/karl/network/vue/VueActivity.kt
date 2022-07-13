package com.karl.network.vue

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.webkit.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.karl.network.R

class VueActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var button: Button

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
        initWeb()
    }

    private fun initWeb() {
        webView = findViewById<WebView>(R.id.web_view).apply {
            clearHistory()
            with(settings) {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            addJavascriptInterface(JsInterface(), "App")
            loadUrl("file:///android_asset/dist/index.html")
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
            println("--------onPageStarted:$url")
            super.onPageFinished(view, url)

        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            println("--------onPageStarted:$url")
            super.onPageStarted(view, url, favicon)
        }


        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val isInterface = request?.url?.let {
                println("---------${it.scheme}")
                println("---------${it.host}")

                it.scheme.equals("js")
            }
            if (isInterface == true) return isInterface
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
    }
}
