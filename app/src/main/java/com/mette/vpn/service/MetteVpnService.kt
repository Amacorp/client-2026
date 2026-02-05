package com.mette.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.v2ray.ang.util.V2rayConfig // پکیج جدید را ایمپورت کن

class MetteVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val configJson = intent?.getStringExtra("V2RAY_JSON")

        if (action == "STOP") {
            stopVpn()
        } else if (configJson != null) {
            startVpn(configJson)
        }
        return START_STICKY
    }

    private fun startVpn(json: String) {
        try {
            val builder = Builder()
                .setSession("MetteVpn")
                .addAddress("10.0.0.1", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .setMtu(1500)
                .addDisallowedApplication(packageName) // جلوگیری از لوپ ترافیک

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                // اجرای هسته در یک ترد جداگانه برای جلوگیری از فریز شدن
                Thread {
                    try {
                        V2rayConfig.startV2ray(this, json)
                    } catch (e: Exception) {
                        Log.e("MetteVPN", "Core Error: ${e.message}")
                    }
                }.start()
            }
        } catch (e: Exception) {
            Log.e("MetteVPN", "VPN Start Error: ${e.message}")
        }
    }

    private fun stopVpn() {
        V2rayConfig.stopV2ray()
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }
}