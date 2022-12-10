package com.karl.network.vue.device

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.karl.network.net.DeviceService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.buffer
import okio.source
import org.json.JSONObject
import java.io.Closeable
import java.net.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.CoroutineContext

class DevicesScannerServer private constructor(private val context: Context) {


    companion object {
        fun getInstance(context: Context): DevicesScannerServer {
            val instance: DevicesScannerServer by lazy {
                DevicesScannerServer(context)
            }
            return instance
        }
    }

    private var mScope: CloseableCoroutineScope = CloseableCoroutineScope()
    private val sendAddress = InetAddress.getByName("224.0.1.77")
    private val serverSocketManager: ServerSocketManager by lazy {
        ServerSocketManager.instance()
    }

    private var udpJob: Job? = null
    private var tcpJob: Job? = null

    fun sendUdp() {
        tcpJob?.apply {
            cancel()
        }
        val flowDevices = flowDevices()
        tcpJob = mScope.launch(Dispatchers.Main) {
            serverSocketManager.initService()
            Log.e("--------------","0------------")
            flowDevices
                .collect {
                    Log.e("!!!:${it.second.first}", "IP: ${it.first} MAC: ${it.second.second}")
                }

        }
        mScope.launch(Dispatchers.Main) {
            Log.e("--------------","1------------")
            flowDevices.collect{
                Log.e("???${it.second.first}", "IP: ${it.first} MAC: ${it.second.second}")
            }
        }

        sendUdpMessage()
    }

    private fun sendUdpMessage() {
        udpJob?.apply {
            cancel()
        }
        udpJob = mScope.launch(Dispatchers.Default) {
            val datagramSocket = DatagramSocket()
            val mapOf =
                mapOf(
                    "ip" to getLocalIp(),
                    "port" to 20000,
                    "requestId" to System.currentTimeMillis()
                )
            val params = "${JSONObject(mapOf)}"
            val json = params.toByteArray()
            val ipByte = getLocalIp().toByteArray()
            val datagramPacket1 = DatagramPacket(ipByte, ipByte.size, sendAddress, 20000)
            val datagramPacket2 = DatagramPacket(json, json.size, sendAddress, 20000)
            datagramSocket.send(datagramPacket1)
            datagramSocket.send(datagramPacket2)
            datagramSocket.close()
        }
    }


    private fun flowDevices(): Flow<Pair<String, Triple<String, String, String>>> =
        flow {
            getIp(this)
        }.filter {
            it.isNotEmpty()
        }.map {
            requestMessage(it)
        }.filter {
            it.second.second.isNotEmpty()
        }.flowOn(Dispatchers.IO)


    private suspend fun getIp(flowCollector: FlowCollector<String>) {
        while (true) {
            val ip = serverSocketManager.ip()
            if (ip != null) {
                Log.e("????????","${flowCollector.hashCode()}")
                flowCollector.emit(ip!!)
            } else {
                break
            }
        }
    }

    private suspend fun requestMessage(ip: String): Pair<String, Triple<String, String, String>> =
        coroutineScope {
            val name = async { DeviceService.service.name(ip) }
            val mac = async { DeviceService.service.mac(ip) }
            val version = async { DeviceService.service.version(ip) }
            Pair(ip, Triple(name.await(), mac.await(), version.await()))
        }


    private fun getLocalIp(): String {
        // 获取WiFi服务
        // 获取WiFi服务
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        return (ipAddress and 0xFF).toString() + "." +
                (ipAddress shr 8 and 0xFF) + "." +
                (ipAddress shr 16 and 0xFF) + "." +
                (ipAddress shr 24 and 0xFF)
    }

    internal class CloseableCoroutineScope() : Closeable, CoroutineScope {
        override val coroutineContext: CoroutineContext =
            SupervisorJob() + Dispatchers.Main.immediate

        override fun close() {
            coroutineContext.cancel()
        }
    }

    fun onDestroy() {
        mScope.close()
    }

    internal class ServerSocketManager {
        private val lock = ReentrantLock()

        companion object {
            private val manager: ServerSocketManager by lazy {
                ServerSocketManager()
            }

            fun instance(): ServerSocketManager = manager
        }

        @Volatile
        private var mServerSocket: ServerSocket? = null

        fun initService() = synchronized(lock) {
            clear()
            if (mServerSocket == null || mServerSocket?.isClosed == false) mServerSocket =
                ServerSocket(20001)
        }

        fun ip(): String? = synchronized(lock) {
            if (mServerSocket == null) return null
            if (mServerSocket?.isClosed == true) return null
            val ip = mServerSocket!!.accept().inetAddress.hostAddress
            return if (!ip.isNullOrEmpty()) ip else ""
        }


        fun clear() = synchronized(lock) {
            mServerSocket?.close()
            mServerSocket = null
        }
    }

}
