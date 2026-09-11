package com.brst.dns.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // DoT (DNS-over-TLS) Hostname & Port
    private val _dotHost = MutableStateFlow(prefs.getString(KEY_DOT_HOST, "1.1.1.1") ?: "1.1.1.1")
    val dotHost: StateFlow<String> = _dotHost.asStateFlow()

    private val _dotPort = MutableStateFlow(prefs.getInt(KEY_DOT_PORT, 853))
    val dotPort: StateFlow<Int> = _dotPort.asStateFlow()

    private val _dotTlsServerName = MutableStateFlow(prefs.getString(KEY_DOT_TLS_NAME, "one.one.one.one") ?: "one.one.one.one")
    val dotTlsServerName: StateFlow<String> = _dotTlsServerName.asStateFlow()

    // Active Protocol: "DoT" or "DoH"
    private val _protocol = MutableStateFlow(prefs.getString(KEY_PROTOCOL, "DoT") ?: "DoT")
    val protocol: StateFlow<String> = _protocol.asStateFlow()

    // DoH URL (Alternative option)
    private val _dohUrl = MutableStateFlow(
        prefs.getString(KEY_DOH_URL, "https://cloudflare-dns.com/dns-query") ?: "https://cloudflare-dns.com/dns-query"
    )
    val dohUrl: StateFlow<String> = _dohUrl.asStateFlow()

    // VPN Active State
    private val _vpnActive = MutableStateFlow(prefs.getBoolean(KEY_VPN_ACTIVE, false))
    val vpnActive: StateFlow<Boolean> = _vpnActive.asStateFlow()

    fun saveDotConfig(host: String, port: Int, tlsName: String = "") {
        val cleanHost = host.trim()
        val cleanName = if (tlsName.isNotBlank()) tlsName.trim() else cleanHost

        prefs.edit()
            .putString(KEY_DOT_HOST, cleanHost)
            .putInt(KEY_DOT_PORT, port)
            .putString(KEY_DOT_TLS_NAME, cleanName)
            .putString(KEY_PROTOCOL, "DoT")
            .apply()

        _dotHost.value = cleanHost
        _dotPort.value = port
        _dotTlsServerName.value = cleanName
        _protocol.value = "DoT"
    }

    fun saveDohConfig(url: String) {
        val cleanUrl = url.trim()
        prefs.edit()
            .putString(KEY_DOH_URL, cleanUrl)
            .putString(KEY_PROTOCOL, "DoH")
            .apply()

        _dohUrl.value = cleanUrl
        _protocol.value = "DoH"
    }

    fun setProtocol(proto: String) {
        prefs.edit().putString(KEY_PROTOCOL, proto).apply()
        _protocol.value = proto
    }

    fun setVpnActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_VPN_ACTIVE, active).apply()
        _vpnActive.value = active
    }

    companion object {
        private const val PREFS_NAME = "brst_dot_dns_prefs"
        private const val KEY_DOT_HOST = "dot_host"
        private const val KEY_DOT_PORT = "dot_port"
        private const val KEY_DOT_TLS_NAME = "dot_tls_name"
        private const val KEY_PROTOCOL = "active_protocol"
        private const val KEY_DOH_URL = "doh_url"
        private const val KEY_VPN_ACTIVE = "vpn_active"
    }
}
