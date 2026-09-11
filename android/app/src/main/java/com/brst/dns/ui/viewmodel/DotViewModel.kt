package com.brst.dns.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brst.dns.BrstDnsApp
import com.brst.dns.data.model.LocalQueryItem
import com.brst.dns.doh.DohVpnService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DotViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = (application as BrstDnsApp).preferences

    val dotHost: StateFlow<String> = preferences.dotHost
    val dotPort: StateFlow<Int> = preferences.dotPort
    val dotTlsServerName: StateFlow<String> = preferences.dotTlsServerName
    val protocol: StateFlow<String> = preferences.protocol
    val dohUrl: StateFlow<String> = preferences.dohUrl

    val isRunning: StateFlow<Boolean> = DohVpnService.isRunning
    val queryCount: StateFlow<Long> = DohVpnService.queryCount
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
            _uiEvent.emit("Preset $name berhasil diterapkan!")
        }
    }
}
