package com.karl.network.okio


import okio.buffer
import okio.sink
import okio.source
import java.net.ServerSocket
import java.net.Socket

internal object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        server()
    }
}

fun server() {
    ServerSocket(9090).use { serverSocket ->
        var socket: Socket?
        while (true) {
            socket = serverSocket.accept()
            val source = socket.source()
            val sink = socket.sink()
            println("接收到客户端的消息：${source.buffer().readUtf8()}")
            /*sink.buffer().writeUtf8("我是服务端").flush()
            socket.shutdownOutput()*/
        }
    }
}
