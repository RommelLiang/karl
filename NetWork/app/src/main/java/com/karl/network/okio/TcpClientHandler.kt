package com.karl.network.okio

import android.util.Log
import java.io.DataInputStream
import java.io.IOException
import java.net.Socket


class TcpClientHandler(private val socket: Socket) : Thread() {
    lateinit var inputStream: DataInputStream
    override fun run() {
        while (true) {
            try {
                inputStream = DataInputStream(socket.getInputStream())
                if(inputStream.available() > 0){
                    Log.e(TAG, "Received: " + inputStream.readUTF())
                    sleep(2000L)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                try {

                    inputStream?.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                try {
                    inputStream?.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    companion object {
        private val TAG = TcpClientHandler::class.java.simpleName
    }

}