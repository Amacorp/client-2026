package com.mette.vpn

import android.net.TrafficStats
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mette.vpn.utils.ConfigParser
import com.mette.vpn.utils.VpnProfile
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {
    private val sheetUrl = "https://docs.google.com/spreadsheets/d/1gKrnhpVbJCOzC_aV-wwdmI5wEn5upVDSPRKv-B-Brj0/export?format=csv&gid=0"

    val vpnProfiles = mutableStateListOf<VpnProfile>()
    val isConnected = mutableStateOf(false)
    val selectedProfile = mutableStateOf<VpnProfile?>(null)

    val downloadSpeed = mutableStateOf("0 KB/s")
    val uploadSpeed = mutableStateOf("0 KB/s")
    val pingStats = mutableStateOf("0 ms")

    val logBuffer = mutableStateListOf<String>()

    // متغیرهای لازم برای محاسبه سرعت واقعی ترافیک
    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0

    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        viewModelScope.launch(Dispatchers.Main) {
            logBuffer.add("[$time] $msg")
            if (logBuffer.size > 200) logBuffer.removeAt(0)
        }
    }

    fun fetchAndSortConfigs() {
        addLog("در حال دریافت لیست سرورها...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val response = client.newCall(Request.Builder().url(sheetUrl).build()).execute()
                val content = response.body?.string() ?: ""

                // پارس کردن لینک‌ها (vless://...)
                val parsed = content.lines()
                    .filter { it.startsWith("vless://") || it.startsWith("vmess://") }
                    .mapNotNull { ConfigParser.parse(it.trim()) }

                if (parsed.isEmpty()) {
                    addLog("هیچ کانفیگ معتبری یافت نشد.")
                    return@launch
                }

                addLog("${parsed.size} سرور یافت شد. در حال محاسبه پینگ...")

                parsed.forEach { it.ping = performTcpPing(it.address, it.port) }
                val sorted = parsed.sortedBy { if (it.ping <= 0) 99999 else it.ping }

                withContext(Dispatchers.Main) {
                    vpnProfiles.clear()
                    vpnProfiles.addAll(sorted)
                    if (selectedProfile.value == null) selectedProfile.value = sorted.firstOrNull()
                    addLog("لیست به‌روز شد. بهترین سرور: ${selectedProfile.value?.name}")
                }
            } catch (e: Exception) {
                addLog("خطا در شبکه: ${e.message}")
            }
        }
    }

    private fun performTcpPing(host: String, port: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 1000) // تایم‌اوت ۱ ثانیه
            socket.close()
            System.currentTimeMillis() - start
        } catch (e: Exception) { -1 }
    }

    fun startStatsUpdate() {
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()

        viewModelScope.launch {
            while (true) {
                if (isConnected.value) {
                    // محاسبه سرعت واقعی بر اساس بایت‌های مصرفی سیستم
                    val currentRxBytes = TrafficStats.getTotalRxBytes()
                    val currentTxBytes = TrafficStats.getTotalTxBytes()

                    val rxSpeed = currentRxBytes - lastRxBytes
                    val txSpeed = currentTxBytes - lastTxBytes

                    downloadSpeed.value = formatSpeed(rxSpeed)
                    uploadSpeed.value = formatSpeed(txSpeed)
                    pingStats.value = "${selectedProfile.value?.ping ?: 0} ms"

                    lastRxBytes = currentRxBytes
                    lastTxBytes = currentTxBytes
                } else {
                    downloadSpeed.value = "0 KB/s"
                    uploadSpeed.value = "0 KB/s"
                    lastRxBytes = TrafficStats.getTotalRxBytes()
                    lastTxBytes = TrafficStats.getTotalTxBytes()
                }
                delay(1000) // به‌روزرسانی ثانیه‌ای برای دقت بیشتر
            }
        }
    }

    private fun formatSpeed(bytes: Long): String {
        if (bytes < 0) return "0 KB/s"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb > 1) {
            String.format("%.1f MB/s", mb)
        } else {
            String.format("%.1f KB/s", kb)
        }
    }
}