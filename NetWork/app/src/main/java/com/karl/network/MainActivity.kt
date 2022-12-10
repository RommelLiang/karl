package com.karl.network

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Window
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.karl.network.view_model.MainViewModel
import com.karl.network.vue.VueActivity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(){
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MainViewModel(this@MainActivity) as T
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("------Activity:Before:","onCreate------")
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        /*val handler = Handler(handlerThread.looper) {
            Log.e("----","${Thread.currentThread().name}")
            Log.e("----","${Thread.currentThread().state}")
            true
        }*/
        /*lifecycleScope.launch {
            Log.e("${Thread.currentThread().name}Flow-------------", "??")
            viewModel.teachF().collect {
                Log.e("${Thread.currentThread().name}Flow-------------", "$it")
            }
        }*/
        findViewById<TextView>(R.id.tv).setOnClickListener {
            //handler.sendEmptyMessage(0)

            /*lifecycleScope.launch {
                viewModel.teachF().collect {
                    Log.e("${Thread.currentThread().name}Flow-------------", "$it")
                }
            }
            *//*viewModel.teach().observe(this) {
                Log.e("${Thread.currentThread().name}LiveData-------------", "$it")
            }*//*
            //startService(Intent(this, ServerService::class.java))
            startActivity(Intent(this, ClientActivity::class.java))*/
            /*Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))*/
            println(this)

            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            ActivityManager.MemoryInfo().also {
                activityManager.getMemoryInfo(it)
                Log.e("---",it.toString())
            }
            startActivity(Intent(this,VueActivity::class.java))
            viewModel.scanner()
        }
        Log.e("------Activity:after:","onCreate------")
    }

    override fun onStart() {
        Log.e("------Activity:Before:","onStart------")
        super.onStart()
        Log.e("------Activity:after:","onStart------")
    }

    override fun onRestart() {
        Log.e("------Activity:Before:","onRestart------")
        super.onRestart()
        Log.e("------Activity:after:","onRestart------")
    }

    override fun onResume() {
        Log.e("------Activity:Before:","onResume------")
        super.onResume()
        Log.e("------Activity:after:","onResume------")
    }

    override fun onPause() {
        Log.e("------Activity:Before:","onPause------")
        super.onPause()
        Log.e("------Activity:after:","onPause------")
    }

    override fun onStop() {
        Log.e("------Activity:Before:","onStop------")
        super.onStop()
        Log.e("------Activity:after:","onStop------")
    }

    override fun onDestroy() {
        Log.e("------Activity:Before:","onDestroy------")
        super.onDestroy()
        Log.e("------Activity:after:","onDestroy------")
    }
}

