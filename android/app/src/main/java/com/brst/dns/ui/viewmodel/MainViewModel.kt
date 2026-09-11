package com.brst.dns.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brst.dns.BrstDnsApp
import com.brst.dns.data.api.AdGuardHomeApi
import com.brst.dns.data.preferences.AppPreferences
import com.brst.dns.data.repository.ServerRepository
import com.brst.dns.doh.DohVpnService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = (application as BrstDnsApp).preferences
    private val api = AdGuardHomeApi(preferences)
    val repository = ServerRepository(api)

    val serverUrl: StateFlow<String> = preferences.serverUrl
    val authType: StateFlow<String> = preferences.authType
    val apiKey: StateFlow<String> = preferences.apiKey
    val username: StateFlow<String> = preferences.username
    val password: StateFlow<String> = preferences.password
    val dohUrl: StateFlow<String> = preferences.dohUrl
    val dohClientId: StateFlow<String> = preferences.dohClientId

    val isDohRunning: StateFlow<Boolean> = DohVpnService.isRunning
    val dohQueryCount: StateFlow<Long> = DohVpnService.queryCount

    val serverStatus = repository.serverStatus
    val serverStats = repository.serverStats
    val gameModeStatus = repository.gameModeStatus
    val queryLogs = repository.queryLogs
    val detectedDevices = repository.detectedDevices
    val isRefreshing = repository.isRefreshing
    val errorMessage = repository.errorMessage

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            repository.refreshDashboard()
            repository.refreshDohInfo()
        }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            repository.refreshQueryLogs()
        }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            repository.refreshDevices()
        }
    }

    fun toggleProtection(enable: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleProtection(enable)
            if (res.isSuccess) {
                _uiEvent.emit(if (enable) "Proteksi server diaktifkan" else "Proteksi server dijeda")
            } else {
                _uiEvent.emit("Gagal mengubah proteksi: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun toggleGameMode() {
        viewModelScope.launch {
            val res = repository.toggleGameMode()
            if (res.isSuccess) {
                val state = res.getOrNull()?.enabled == true
                _uiEvent.emit(if (state) "Smart Game Mode QoS AKTIF" else "Smart Game Mode QoS NONAKTIF")
            } else {
                _uiEvent.emit("Gagal mengubah Game Mode: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun runMaintenance() {
        viewModelScope.launch {
            val res = repository.runMaintenance()
            if (res.isSuccess) {
                val data = res.getOrNull()
                _uiEvent.emit("Maintenance selesai! Memori dibebaskan: ${data?.freedMemoryMb ?: 0.0} MB")
                repository.refreshDashboard()
            } else {
                _uiEvent.emit("Gagal maintenance: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun unblockDomain(domain: String) {
        viewModelScope.launch {
            val res = repository.unblockDomain(domain)
            if (res.isSuccess) {
                _uiEvent.emit("Domain $domain berhasil di-unblock (whitelist dibuat)!")
                repository.refreshQueryLogs()
            } else {
                _uiEvent.emit("Gagal unblock: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun blockDomain(domain: String) {
        viewModelScope.launch {
            val res = repository.blockDomain(domain)
            if (res.isSuccess) {
                _uiEvent.emit("Domain $domain berhasil diblokir!")
                repository.refreshQueryLogs()
            } else {
                _uiEvent.emit("Gagal memblokir domain: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun saveApiKeySettings(url: String, apiKey: String) {
        preferences.saveApiKeyConfig(url, apiKey)
        viewModelScope.launch {
            _uiEvent.emit("Mode API Key disimpan, menghubungkan ke server...")
            repository.refreshDashboard()
        }
    }

    fun saveBasicAuthSettings(url: String, user: String, pass: String) {
        preferences.saveBasicAuthConfig(url, user, pass)
        viewModelScope.launch {
            _uiEvent.emit("Mode Basic Auth disimpan, menghubungkan ke server...")
            repository.refreshDashboard()
        }
    }

    fun saveDohSettings(url: String, clientId: String) {
        preferences.saveDohConfig(url, clientId)
        viewModelScope.launch {
            _uiEvent.emit("Pengaturan DoH endpoint berhasil diperbarui")
        }
    }
}
