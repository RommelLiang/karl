package com.karl.network.socket

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import java.net.ServerSocket


class ServerService : Service() {

    override fun onCreate() {
        super.onCreate()

        startServer()

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startServer() {
        val serverSocket = ServerSocket(2020, 1)
        Thread {
            while (true) {

                val socket = serverSocket.accept()
                val inputStream = socket.getInputStream()
                Log.e("services", socket.inetAddress.hostAddress)
                val readBytes = ByteArray(1024)

                var msgLen: Int
                val stringBuilder = StringBuilder()

                while (inputStream.read(readBytes).also { msgLen = it } != -1) {
                    stringBuilder.append(String(readBytes, 0, msgLen))
                }

                Log.e("--------", "get message from client: $stringBuilder")

                socket.getOutputStream().apply {
                    write("$stringBuilder 一定会胜利的".toByteArray())
                    flush()
                }
                inputStream.close()

            }
        }.start()
    }
}