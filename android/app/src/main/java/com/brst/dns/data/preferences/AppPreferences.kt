package com.brst.dns.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _serverUrl = MutableStateFlow(prefs.getString(KEY_SERVER_URL, "http://192.168.1.1:3000") ?: "")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _username = MutableStateFlow(prefs.getString(KEY_USERNAME, "admin") ?: "")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow(prefs.getString(KEY_PASSWORD, "") ?: "")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _dohUrl = MutableStateFlow(
        prefs.getString(KEY_DOH_URL, "https://cloudflare-dns.com/dns-query") ?: "https://cloudflare-dns.com/dns-query"
    )
    val dohUrl: StateFlow<String> = _dohUrl.asStateFlow()

    private val _dohClientId = MutableStateFlow(prefs.getString(KEY_DOH_CLIENT_ID, "Android-Device") ?: "Android-Device")
    val dohClientId: StateFlow<String> = _dohClientId.asStateFlow()

    private val _dohVpnActive = MutableStateFlow(prefs.getBoolean(KEY_DOH_VPN_ACTIVE, false))
    val dohVpnActive: StateFlow<Boolean> = _dohVpnActive.asStateFlow()

    fun saveServerConfig(url: String, user: String, pass: String) {
        val cleanUrl = cleanUrl(url)

        prefs.edit()
            .putString(KEY_SERVER_URL, cleanUrl)
            .putString(KEY_USERNAME, user.trim())
            .putString(KEY_PASSWORD, pass)
            .apply()

        _serverUrl.value = cleanUrl
        _username.value = user.trim()
        _password.value = pass
    }

    fun saveDohConfig(url: String, clientId: String) {
        val cleanUrl = url.trim()
        val cleanClient = clientId.trim()

        prefs.edit()
            .putString(KEY_DOH_URL, cleanUrl)
            .putString(KEY_DOH_CLIENT_ID, cleanClient)
            .apply()

        _dohUrl.value = cleanUrl
        _dohClientId.value = cleanClient
    }

    fun setDohVpnActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_DOH_VPN_ACTIVE, active).apply()
        _dohVpnActive.value = active
    }

    fun getEffectiveDohEndpoint(): String {
        val base = _dohUrl.value.trimEnd('/')
        val client = _dohClientId.value.trim()
        return if (client.isNotEmpty() && base.endsWith("/dns-query")) {
            "$base/$client"
        } else {
            base
        }
    }

    private fun cleanUrl(url: String): String {
        return if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }.trimEnd('/')
    }

    companion object {
        private const val PREFS_NAME = "brst_dns_preferences"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_DOH_URL = "doh_url"
        private const val KEY_DOH_CLIENT_ID = "doh_client_id"
        private const val KEY_DOH_VPN_ACTIVE = "doh_vpn_active"
    }
}
