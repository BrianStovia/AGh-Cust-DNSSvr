package com.brst.dns.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brst.dns.BrstDnsApp
import com.brst.dns.data.api.AdGuardHomeApi
import com.brst.dns.data.api.DnsConfig
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

    // Local Preferences State
    val serverUrl: StateFlow<String> = preferences.serverUrl
    val username: StateFlow<String> = preferences.username
    val password: StateFlow<String> = preferences.password
    val dohUrl: StateFlow<String> = preferences.dohUrl
    val dohClientId: StateFlow<String> = preferences.dohClientId

    // DoH VPN Engine State
    val isDohRunning: StateFlow<Boolean> = DohVpnService.isRunning
    val dohQueryCount: StateFlow<Long> = DohVpnService.queryCount

    // Server State
    val serverStatus = repository.serverStatus
    val serverStats = repository.serverStats
    val gameModeStatus = repository.gameModeStatus
    val queryLogs = repository.queryLogs
    val detectedDevices = repository.detectedDevices
    val isRefreshing = repository.isRefreshing
    val errorMessage = repository.errorMessage

    // Extended Server Settings
    val dnsConfig = repository.dnsConfig
    val filteringStatus = repository.filteringStatus
    val safeBrowsingEnabled = repository.safeBrowsingEnabled
    val parentalEnabled = repository.parentalEnabled
    val safeSearchEnabled = repository.safeSearchEnabled
    val rebindConfig = repository.rebindConfig
    val odohConfig = repository.odohConfig
    val telegramConfig = repository.telegramConfig
    val blockedServices = repository.blockedServices

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            repository.refreshDashboard()
            repository.refreshDohInfo()
            repository.loadAllSettings()
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            repository.loadAllSettings()
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

    fun updateGameMode(enabled: Boolean) {
        viewModelScope.launch {
            val res = repository.setGameMode(enabled)
            if (res.isSuccess) {
                _uiEvent.emit(if (enabled) "Smart Game Mode QoS AKTIF" else "Smart Game Mode QoS NONAKTIF")
            }
        }
    }

    fun updateDnsConfig(
        upstreams: List<String>,
        bootstraps: List<String>,
        upstreamMode: String,
        dnssec: Boolean,
        ecs: Boolean,
        rateLimit: Int,
        optimistic: Boolean
    ) {
        viewModelScope.launch {
            val current = dnsConfig.value ?: DnsConfig()
            val newConfig = current.copy(
                upstreamDns = upstreams,
                bootstrapDns = bootstraps,
                upstreamMode = upstreamMode,
                dnssecEnabled = dnssec,
                ednsCsEnabled = ecs,
                ratelimit = rateLimit,
                cacheOptimistic = optimistic
            )
            val res = repository.saveDnsConfig(newConfig)
            if (res.isSuccess) {
                _uiEvent.emit("Pengaturan DNS & Upstream berhasil disimpan!")
            } else {
                _uiEvent.emit("Gagal menyimpan DNS config: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun updateFiltering(enabled: Boolean, interval: Long) {
        viewModelScope.launch {
            val res = repository.setFilteringEnabled(enabled, interval)
            if (res.isSuccess) {
                _uiEvent.emit("Pengaturan filtering iklan diperbarui!")
            }
        }
    }

    fun updateUserRules(rules: List<String>) {
        viewModelScope.launch {
            val res = repository.saveUserRules(rules)
            if (res.isSuccess) {
                _uiEvent.emit("Aturan kustom (${rules.size} rules) berhasil disimpan!")
            } else {
                _uiEvent.emit("Gagal menyimpan aturan: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun toggleSafeBrowsing(enabled: Boolean) {
        viewModelScope.launch {
            val res = repository.setSafeBrowsing(enabled)
            if (res.isSuccess) {
                _uiEvent.emit("SafeBrowsing (Malware Shield): ${if (enabled) "AKTIF" else "NONAKTIF"}")
            }
        }
    }

    fun toggleParental(enabled: Boolean) {
        viewModelScope.launch {
            val res = repository.setParental(enabled)
            if (res.isSuccess) {
                _uiEvent.emit("Parental Control: ${if (enabled) "AKTIF" else "NONAKTIF"}")
            }
        }
    }

    fun toggleSafeSearch(enabled: Boolean) {
        viewModelScope.launch {
            val res = repository.setSafeSearch(enabled)
            if (res.isSuccess) {
                _uiEvent.emit("SafeSearch Filter: ${if (enabled) "AKTIF" else "NONAKTIF"}")
            }
        }
    }

    fun updateRebindConfig(enabled: Boolean, strict: Boolean, whitelist: List<String>) {
        viewModelScope.launch {
            val res = repository.saveRebindConfig(enabled, strict, whitelist)
            if (res.isSuccess) {
                _uiEvent.emit("Anti-DNS Rebinding Shield diperbarui!")
            }
        }
    }

    fun updateODoHConfig(enabled: Boolean, preset: String, relay: String, target: String) {
        viewModelScope.launch {
            val res = repository.saveODoHConfig(enabled, preset, relay, target)
            if (res.isSuccess) {
                _uiEvent.emit("Pengaturan ODoH Shield disimpan!")
            }
        }
    }

    fun updateTelegramConfig(enabled: Boolean, token: String, chatId: String) {
        viewModelScope.launch {
            val res = repository.saveTelegramConfig(enabled, token, chatId)
            if (res.isSuccess) {
                _uiEvent.emit("Pengaturan Telegram Bot berhasil disimpan!")
            }
        }
    }

    fun toggleBlockedService(serviceName: String, block: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleBlockedService(serviceName, block)
            if (res.isSuccess) {
                _uiEvent.emit("Layanan $serviceName ${if (block) "DIBLOKIR" else "DIBUKA"}")
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

    fun saveServerSettings(url: String, user: String, pass: String) {
        preferences.saveServerConfig(url, user, pass)
        viewModelScope.launch {
            _uiEvent.emit("Pengaturan server berhasil disimpan, menghubungkan...")
            repository.refreshDashboard()
            repository.loadAllSettings()
        }
    }

    fun saveDohSettings(url: String, clientId: String) {
        preferences.saveDohConfig(url, clientId)
        viewModelScope.launch {
            _uiEvent.emit("Pengaturan DoH endpoint berhasil diperbarui")
        }
    }
}
