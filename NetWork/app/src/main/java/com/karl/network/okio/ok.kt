package com.karl.network.okio

import okio.BufferedSource
import okio.Okio
import okio.Source
import java.io.*
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock

class Ok {
}

fun main() {
 val file =
        File("/Users/chuangkegongchang/karl/NetWork/app/src/main/java/com/karl/network/okio/file.text")

    /*val buffer = file.source().buffer()
    println(String(byteArrayOf(buffer.readByte()), Charset.forName("UTF-8")))

    println(buffer.readUtf8())


    file.sink().buffer().writeString("中国啊",Charset.forName("UTF-8")).close()
    println(ByteArrayInputStream("中国啊".toByteArray(Charset.forName("UTF-8"))).read())*/
    //println("$fileInputStream:${String(byteArrayOf(fileInputStream.toByte()),Charset.forName("UTF-8"))}")

    //buffer.close()

    /*ObjectOutputStream(FileOutputStream(file)).writeUTF("asdf")
    println(ObjectInputStream(FileInputStream(file)).readUTF())*/
    val atomicInteger = AtomicInteger()
    atomicInteger.set(0)
    println(atomicInteger.get())
    atomicInteger.getAndIncrement()
    println(atomicInteger.get())

    println(atomicInteger.getAndSet(3))


}

