package com.brst.dns.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brst.dns.BrstDnsApp
import com.brst.dns.data.blocklist.LocalBlocklistManager
import com.brst.dns.data.model.LocalQueryItem
import com.brst.dns.doh.DohVpnService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DotViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = (application as BrstDnsApp).preferences
    private val blocklistManager = LocalBlocklistManager.getInstance(application)

    val dotHost: StateFlow<String> = preferences.dotHost
    val dotPort: StateFlow<Int> = preferences.dotPort
    val dotTlsServerName: StateFlow<String> = preferences.dotTlsServerName
    val protocol: StateFlow<String> = preferences.protocol
    val dohUrl: StateFlow<String> = preferences.dohUrl

    // Blocklist State
    val isBlocklistEnabled: StateFlow<Boolean> = preferences.isBlocklistEnabled
    val blocklistUrl: StateFlow<String> = preferences.blocklistUrl
    val customBlockedRules: StateFlow<String> = preferences.customBlockedRules
    val blocklistRuleCount: StateFlow<Int> = blocklistManager.ruleCount
    val isUpdatingBlocklist: StateFlow<Boolean> = blocklistManager.isUpdating

    val isRunning: StateFlow<Boolean> = DohVpnService.isRunning
    val queryCount: StateFlow<Long> = DohVpnService.queryCount
    val blockedCount: StateFlow<Long> = DohVpnService.blockedCount
    val recentQueries: StateFlow<List<LocalQueryItem>> = DohVpnService.recentQueriesList

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    fun saveDotConfig(host: String, port: Int, tlsName: String = "") {
        preferences.saveDotConfig(host, port, tlsName)
        viewModelScope.launch {
            _uiEvent.emit("Konfigurasi DoT ($host:$port) berhasil disimpan!")
        }
    }

    fun saveDohConfig(url: String) {
        preferences.saveDohConfig(url)
        viewModelScope.launch {
            _uiEvent.emit("Konfigurasi DoH berhasil disimpan!")
        }
    }

    fun setProtocol(proto: String) {
        preferences.setProtocol(proto)
        viewModelScope.launch {
            _uiEvent.emit("Protokol diubah ke $proto")
        }
    }

    fun applyPreset(name: String, host: String, port: Int, tlsName: String) {
        preferences.saveDotConfig(host, port, tlsName)
        viewModelScope.launch {
            _uiEvent.emit("Preset DoT $name berhasil diterapkan!")
        }
    }

    fun applyDohPreset(name: String, url: String) {
        preferences.saveDohConfig(url)
        viewModelScope.launch {
            _uiEvent.emit("Preset DoH $name berhasil diterapkan!")
        }
    }

    fun setBlocklistEnabled(enabled: Boolean) {
        preferences.setBlocklistEnabled(enabled)
        blocklistManager.reloadRules()
        viewModelScope.launch {
            _uiEvent.emit(if (enabled) "Pemblokir Iklan & Pelacak Aktif" else "Pemblokir Iklan Dinonaktifkan")
        }
    }

    fun updateBlocklistFromUrl(url: String) {
        viewModelScope.launch {
            val result = blocklistManager.updateFromUrl(url)
            if (result.isSuccess) {
                _uiEvent.emit("Daftar blokir diperbarui! (${result.getOrNull()} aturan aktif)")
            } else {
                _uiEvent.emit("Gagal: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun saveCustomRules(rules: String) {
        preferences.setCustomBlockedRules(rules)
        blocklistManager.reloadRules()
        viewModelScope.launch {
            _uiEvent.emit("Aturan kustom berhasil disimpan!")
        }
    }
}
