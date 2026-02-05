package com.mette.vpn.utils

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object ConfigParser {
    fun parse(config: String): VpnProfile? {
        return try {
            val uri = Uri.parse(config)
            val host = uri.host ?: ""
            val port = if (uri.port != -1) uri.port else 443
            val uuid = uri.userInfo ?: ""

            val sni = uri.getQueryParameter("sni") ?: ""
            val pbk = uri.getQueryParameter("pbk") ?: ""
            val sid = uri.getQueryParameter("sid") ?: ""
            val flow = uri.getQueryParameter("flow") ?: ""

            val json = JSONObject()

            // 1. Inbounds - پل ارتباطی با لایه TUN اندروید
            val inbounds = JSONArray().put(JSONObject().apply {
                put("tag", "socks-in")
                put("port", 10808) // پورت استاندارد برای tun2socks
                put("listen", "127.0.0.1")
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("udp", true)
                    put("auth", "noauth")
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().put("http").put("tls").put("quic"))
                    put("routeOnly", false)
                })
            })
            json.put("inbounds", inbounds)

            // 2. Outbounds - تنظیمات Reality و Freedom
            val outbounds = JSONArray()

            // خروجی اصلی پروکسی
            outbounds.put(JSONObject().apply {
                put("protocol", "vless")
                put("tag", "proxy")
                put("settings", JSONObject().apply {
                    put("vnext", JSONArray().put(JSONObject().apply {
                        put("address", host)
                        put("port", port)
                        put("users", JSONArray().put(JSONObject().apply {
                            put("id", uuid)
                            put("encryption", "none")
                            if (flow.isNotEmpty()) put("flow", flow)
                        }))
                    }))
                })
                put("streamSettings", JSONObject().apply {
                    put("network", "tcp")
                    put("security", "reality")
                    put("realitySettings", JSONObject().apply {
                        put("fingerprint", "chrome")
                        put("serverName", sni)
                        put("publicKey", pbk)
                        put("shortId", sid)
                        put("spiderX", "/")
                    })
                })
            })

            // خروجی مستقیم برای DNS و سایت‌های داخلی
            outbounds.put(JSONObject().apply {
                put("protocol", "freedom")
                put("tag", "direct")
                put("settings", JSONObject().apply {
                    put("domainStrategy", "UseIP")
                })
            })

            // خروجی مسدود سازی (برای حذف تبلیغات یا موارد دیگر)
            outbounds.put(JSONObject().apply {
                put("protocol", "blackhole")
                put("tag", "block")
            })

            json.put("outbounds", outbounds)

            // 3. DNS - تنظیمات حیاتی برای رفع فیلترینگ اینستاگرام
            json.put("dns", JSONObject("""
                {
                    "tag": "dns_inbound",
                    "hosts": { "domain:googleapis.cn": "googleapis.com" },
                    "servers": [
                        "1.1.1.1",
                        "8.8.8.8",
                        {
                            "address": "https://1.1.1.1/dns-query",
                            "domains": ["geosite:google", "geosite:facebook", "geosite:twitter"]
                        },
                        "localhost"
                    ]
                }
            """.trimIndent()))

            // 4. Routing - هدایت هوشمند ترافیک
            json.put("routing", JSONObject("""
                {
                    "domainStrategy": "IPIfNonMatch",
                    "rules": [
                        { "type": "field", "port": "53", "outboundTag": "direct" },
                        { "type": "field", "protocol": ["dns"], "outboundTag": "direct" },
                        { "type": "field", "domain": ["geosite:ir", "geosite:private"], "outboundTag": "direct" },
                        { "type": "field", "outboundTag": "proxy", "network": "tcp,udp" }
                    ]
                }
            """.trimIndent()))

            return VpnProfile(
                name = "Mette Reality",
                address = host,
                port = port,
                type = "VLESS",
                flag = "🛡️",
                rawConfig = config,
                fullJson = json.toString()
            )
        } catch (e: Exception) { null }
    }
}