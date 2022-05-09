package com.karl.network.socket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.karl.network.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.Socket

class ClientActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        findViewById<Button>(R.id.button).setOnClickListener {
            start()
        }
    }

    private fun start() {
        lifecycleScope.launch(Dispatchers.IO){
            val socket = Socket("127.0.0.1", 2020)
            try {
                if(!socket.isConnected) {
                    socket.connect(socket.remoteSocketAddress)
                }
                val outputStream = socket.getOutputStream()
                outputStream.write("china".toByteArray())
                outputStream.flush()
                socket.shutdownOutput()
                val inputStream = socket.getInputStream()
                val inputStreamReader = InputStreamReader(inputStream)
                inputStreamReader.readLines().forEach {
                    Log.e("日你妈退钱","$it")
                }
                var buffer = ByteArray(1000)
                var a = 0
                var stringBuilder = StringBuilder()
                while (inputStream.read(buffer).also { a = it } != -1) {
                    stringBuilder.append(String(buffer, 0, a))
                }
                Log.e("未来告诉了我们","$stringBuilder")
            } catch (e:Exception){
                Log.e("ppp",e.toString())
            } finally {
                socket.close()
            }
        }
    }
}