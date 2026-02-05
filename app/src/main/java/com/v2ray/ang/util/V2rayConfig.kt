package com.v2ray.ang.util // این خط باید دقیقاً همین باشد

import android.content.Context
import android.util.Log

object V2rayConfig {
    // این نام‌ها باید دقیقاً مطابق با توابع درون libgojni.so باشند
    @JvmStatic
    external fun startV2ray(context: Context, configJson: String): Long
    
    @JvmStatic
    external fun stopV2ray()
    
    @JvmStatic
    external fun getCoreVersion(): String

    init {
        try {
            System.loadLibrary("gojni")
            System.loadLibrary("tun2socks")
            Log.d("MetteVPN", "Native Libraries Loaded Successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("MetteVPN", "Link Error: Check if Package Name is com.v2ray.ang")
        }
    }
}