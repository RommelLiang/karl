package com.karl.network.view_model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karl.network.net.GitHubService
import com.karl.network.okio.TcpClientHandler
import com.karl.network.okio.TcpServerService
import com.karl.network.vue.device.DevicesScannerServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Okio
import okio.buffer
import okio.sink
import okio.source
import org.json.JSONObject
import java.io.DataInputStream
import java.net.*
import java.nio.charset.StandardCharsets


class MainViewModel(private val context: Context) : ViewModel() {

    fun teach() = GitHubService.service.listReposLiveData("RommelLiang")
    fun teachF() = GitHubService.service.listReposFlow("RommelLiang")
    fun test() {
        viewModelScope.launch {
            println("viewModelScope thread name : ${Thread.currentThread().name}")
        }
    }

    private var register: Intent? = null
    private val wifiManager: WifiManager by lazy {
        context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private var devicesScannerServer: DevicesScannerServer? = null

    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                wifiManager.scanResults.forEach {
                    Log.e("WIFI：${it.BSSID}", it.SSID)
                }
            } else {

            }
        }
    }

    @JavascriptInterface
    fun scanWifi() {
        register?.apply {
            context?.unregisterReceiver(wifiScanReceiver)
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        val registerReceiver = context?.registerReceiver(wifiScanReceiver, intentFilter)
        val success = wifiManager.startScan()
    }

    fun scanner() {
        val instance = DevicesScannerServer.getInstance(context)
        instance.sendUdp()
    }

    override fun onCleared() {
        devicesScannerServer?.onDestroy()
        super.onCleared()
    }

    fun udpBroadCast() {
        val datagramSocket = DatagramSocket()
        //val sendAddress = InetAddress.getByName("10.10.202.36")
        val sendAddress = InetAddress.getByName("224.0.1.77")
        Log.e("--------", getLocalIp())
        val mapOf =
            mapOf("ip" to getLocalIp(), "port" to 20000, "requestId" to System.currentTimeMillis())
        val toString = "${JSONObject(mapOf).toString()}"
        val json = toString.toByteArray()
        Log.e("'--------'", toString)
        val toByteArray = getLocalIp().toByteArray()
        val datagramPacket1 = DatagramPacket(toByteArray, toByteArray.size, sendAddress, 20000)
        val datagramPacket2 = DatagramPacket(json, json.size, sendAddress, 20000)
        tcp()
        viewModelScope.launch(Dispatchers.Default) {
            launch(Dispatchers.IO) { receive() }

            datagramSocket.send(datagramPacket1)
            datagramSocket.send(datagramPacket2)
            datagramSocket.close()
        }
    }

    private fun receive() {
        val datagramSocket = DatagramSocket(20000)
        while (true) {
            val inBuf = ByteArray(1024)
            val datagramPacket = DatagramPacket(inBuf, inBuf.size)
            datagramSocket.receive(datagramPacket)
            val data = datagramPacket.data
            val string = String(data)
            Log.e("收到了:${datagramPacket}", string)
        }
    }

    private fun tcp() {

        viewModelScope.launch(Dispatchers.IO) {
            ServerSocket(20001).use { serverSocket ->
                var socket: Socket?
                while (true) {
                    socket = serverSocket.accept()

                    Thread {

                        while (true) {
                            val readUtf8 = socket.source().buffer().readUtf8()
                            Log.e("${socket.inetAddress.hostAddress}-----------:", readUtf8)
                            break

                        }
                    }.start()

                }
            }
        }
        context.startService(Intent(context, TcpServerService::class.java))
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
}