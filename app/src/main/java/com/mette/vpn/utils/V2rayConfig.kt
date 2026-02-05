package com.mette.vpn.utils

import android.content.Context
import android.util.Log

object V2rayConfig {
    // توابع بومی (Native) موجود در فایل‌های .so
    external fun startV2ray(context: Context, configJson: String): Long
    external fun stopV2ray()
    external fun getCoreVersion(): String

    init {
        try {
            System.loadLibrary("gojni")
            System.loadLibrary("tun2socks")
            Log.d("MetteVPN", "Native Libraries Loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("MetteVPN", "Critical Error: Native libraries not found or package name mismatch!")
        }
    }
}