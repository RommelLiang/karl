package com.karl.network.okio

import okio.*
import java.io.*
import java.net.Socket
import java.util.concurrent.TimeUnit

val pathText = "${File("").absoluteFile}/app/src/main/java/com/karl/network/okio/file.text"
val pathInt = "${File("").absoluteFile}/app/src/main/java/com/karl/network/okio/int.text"
val pathString = "${File("").absoluteFile}/app/src/main/java/com/karl/network/okio/string.text"

fun main() {
    //ioRead()
    //okIoRead()
    //readInt()
    //timeOut()
    //okIoWrite()
    //ioCopy()
    //okIoCopy()
    //ioPipe()
    okIoPope()
}


fun ioCopy() {
    val inputStream = FileInputStream(File(pathText))
    val outPutStream = FileOutputStream(File(pathString))
    val bytes = ByteArray(1024)
    var size: Int
    while (inputStream.read(bytes).also { size = it } != -1) {
        outPutStream.write(bytes, 0, size)
    }
    outPutStream.flush()
}


fun okIoCopy() {
    val sink = File(pathString).sink().buffer()
    val source = File(pathText).source()
    sink.writeAll(source)
    sink.flush()
}

fun ioRead() {
    var inputStream: FileInputStream? = null
    try {
        val file = File(pathText)//创建文件
        inputStream = FileInputStream(file)//创建输入流
        val readBytes = inputStream.readBytes()//读取数据
        val text = String(readBytes, Charsets.UTF_8)//获取文本信息
        println(text)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        inputStream?.close()//关闭输入流
    }
}

fun okIoRead() {
    /* var source: Source? = null
     try {
         val file = File(path)//创建文件
         source = Okio.source(file)//创建source
         val buffer = Okio.buffer(source)//创建buffer
         val readUtf8 = buffer.readUtf8()//读取文本
         println(readUtf8)
     } catch (e: IOException) {
         e.printStackTrace()
     } finally {
         source?.close()
     }*/
    var source: Source? = null
    try {
        val file = File(pathText)//创建文件
        //file.source().buffer().readUtf8()
        source = file.source().apply {
            println(buffer().readUtf8())
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        source?.close()
    }

}

fun okIoWrite() {
    File(pathString).sink().buffer()
        .writeUtf8("A spectre is haunting Europe — the spectre of communism")
}

fun chainExecution() {
    val readUtf8 = File(pathText).source().buffer().readUtf8()
    val readUtf81 = Socket().source().buffer().readUtf8()
    val readUtf82 = FileInputStream(File(pathText)).source().buffer().readUtf8()

    File(pathText).sink().buffer().writeUtf8("文本信息")
    Socket().sink().buffer().writeUtf8("文本信息")
    FileOutputStream(File(pathText)).sink().buffer().writeUtf8("文本信息")
}

fun readInt() {
    val dataInputStream = DataInputStream(
        FileInputStream(File(pathInt))
    )
    val num = dataInputStream.readInt()
    println("$num")
    val readInt = File(pathInt).source().buffer().readInt()
    println(readInt)

    File(pathString).sink().buffer()
        .writeUtf8("A spectre is haunting Europe — the spectre of communism").flush()
}

fun timeOut() {
    val socket = Socket("localhost", 9090)
    try {
        socket.sink().buffer().writeUtf8("我是客户端").flush()
        socket.shutdownOutput()
        val source = socket.source()
        source.timeout().timeout(2000, TimeUnit.MILLISECONDS)
        println("来自服务端的消息：${source.buffer().readUtf8()}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun ioPipe() {
    val pipedOutputStream = PipedOutputStream()
    val pipedInputStream = PipedInputStream(pipedOutputStream)
    Thread {
        pipedOutputStream.write("A spectre is haunting Europe — the spectre of communism".toByteArray())
    }.start()
    Thread {
        val bytes = ByteArray(1024)
        val size = pipedInputStream.read()
        pipedInputStream.read(bytes, 0, size)
        println(String(bytes, 0, size))
    }.start()
}

fun okIoPope() {
    val pipe = Pipe(1024)

    Thread {
        pipe.sink.buffer().apply {
            writeUtf8("A spectre is haunting Europe — the spectre of communism")
            close()
        }
    }.start()

    Thread {
        val readUtf8 = pipe.source.buffer().readUtf8()
        println(readUtf8)
    }.start()
}



