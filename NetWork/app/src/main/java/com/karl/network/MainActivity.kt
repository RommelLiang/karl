package com.karl.network

import android.content.Intent
import android.os.Bundle
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Window
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.karl.network.view_model.MainViewModel
import com.karl.network.vue.VueActivity

class MainActivity : AppCompatActivity(){
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        /*val handler = Handler(handlerThread.looper) {
            Log.e("----","${Thread.currentThread().name}")
            Log.e("----","${Thread.currentThread().state}")
            true
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

            startActivity(Intent(this,VueActivity::class.java))

        }
    }
}

