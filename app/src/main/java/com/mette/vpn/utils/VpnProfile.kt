package com.mette.vpn.utils

data class VpnProfile(
    val name: String,
    val address: String,
    val port: Int,
    val type: String,
    val flag: String,
    val rawConfig: String,
    var ping: Long = -1,
    val fullJson: String = "" // محتوای اصلی برای هسته Xray
)