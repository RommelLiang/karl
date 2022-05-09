package com.karl.network

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.karl.network.socket.ClientActivity
import com.karl.network.socket.ServerService
import com.karl.network.view_model.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        windowManager
        findViewById<TextView>(R.id.tv).setOnClickListener {
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
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
            Glide.with(this).load("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web/img/default.640d9a7.png").into(findViewById(R.id.imageView))
        }


    }
}
