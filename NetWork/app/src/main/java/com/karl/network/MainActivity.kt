package com.karl.network

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.karl.network.view_model.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tv).setOnClickListener {
            /*lifecycleScope.launch {
            viewModel.teachF().collect {
                Log.e("${Thread.currentThread().name}Flow-------------", "$it")
            }
        }*/
            viewModel.teach().observe(this) {
                Log.e("${Thread.currentThread().name}LiveData-------------", "$it")
            }
        }
    }
}