package com.brst.dns.data.repository

import com.brst.dns.data.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServerRepository(private val api: AdGuardHomeApi) {

    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus: StateFlow<ServerStatus?> = _serverStatus.asStateFlow()

    private val _serverStats = MutableStateFlow<ServerStats?>(null)
    val serverStats: StateFlow<ServerStats?> = _serverStats.asStateFlow()

    private val _dnsConfig = MutableStateFlow<DnsConfig?>(null)
    val dnsConfig: StateFlow<DnsConfig?> = _dnsConfig.asStateFlow()

    private val _filteringStatus = MutableStateFlow<FilteringStatus?>(null)
    val filteringStatus: StateFlow<FilteringStatus?> = _filteringStatus.asStateFlow()

    private val _safeBrowsingEnabled = MutableStateFlow(false)
    val safeBrowsingEnabled: StateFlow<Boolean> = _safeBrowsingEnabled.asStateFlow()

    private val _parentalEnabled = MutableStateFlow(false)
    val parentalEnabled: StateFlow<Boolean> = _parentalEnabled.asStateFlow()

    private val _safeSearchEnabled = MutableStateFlow(false)
    val safeSearchEnabled: StateFlow<Boolean> = _safeSearchEnabled.asStateFlow()

    private val _rebindConfig = MutableStateFlow<RebindConfig?>(null)
    val rebindConfig: StateFlow<RebindConfig?> = _rebindConfig.asStateFlow()

    private val _odohConfig = MutableStateFlow<ODoHConfig?>(null)
    val odohConfig: StateFlow<ODoHConfig?> = _odohConfig.asStateFlow()

    private val _telegramConfig = MutableStateFlow<TelegramConfig?>(null)
    val telegramConfig: StateFlow<TelegramConfig?> = _telegramConfig.asStateFlow()

    private val _blockedServices = MutableStateFlow<List<String>>(emptyList())
    val blockedServices: StateFlow<List<String>> = _blockedServices.asStateFlow()

    private val _gameModeStatus = MutableStateFlow<GameModeStatus?>(null)
    val gameModeStatus: StateFlow<GameModeStatus?> = _gameModeStatus.asStateFlow()

    private val _queryLogs = MutableStateFlow<QueryLogResponse?>(null)
    val queryLogs: StateFlow<QueryLogResponse?> = _queryLogs.asStateFlow()

    private val _detectedDevices = MutableStateFlow<DetectedDevicesResponse?>(null)
    val detectedDevices: StateFlow<DetectedDevicesResponse?> = _detectedDevices.asStateFlow()

    private val _dohInfo = MutableStateFlow<DohInfoResponse?>(null)
    val dohInfo: StateFlow<DohInfoResponse?> = _dohInfo.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    suspend fun refreshDashboard() {
        _isRefreshing.value = true
        _errorMessage.value = null

        api.getStatus().onSuccess { _serverStatus.value = it }
            .onFailure { _errorMessage.value = it.localizedMessage }

        api.getStats().onSuccess { _serverStats.value = it }
        api.getGameModeStatus().onSuccess { _gameModeStatus.value = it }

        _isRefreshing.value = false
    }

    suspend fun loadAllSettings() {
        _isRefreshing.value = true

        api.getDnsConfig().onSuccess { _dnsConfig.value = it }
        api.getFilteringStatus().onSuccess { _filteringStatus.value = it }
        api.getSafeBrowsingStatus().onSuccess { _safeBrowsingEnabled.value = it.enabled }
        api.getParentalStatus().onSuccess { _parentalEnabled.value = it.enabled }
        api.getSafeSearchStatus().onSuccess { _safeSearchEnabled.value = it.enabled }
        api.getRebindConfig().onSuccess { _rebindConfig.value = it }
        api.getODoHConfig().onSuccess { _odohConfig.value = it }
        api.getTelegramConfig().onSuccess { _telegramConfig.value = it }
        api.getBlockedServices().onSuccess { _blockedServices.value = it.blockedServices }

        _isRefreshing.value = false
    }

    suspend fun saveDnsConfig(config: DnsConfig): Result<String> {
        val res = api.setDnsConfig(config)
        if (res.isSuccess) {
            _dnsConfig.value = config
        }
        return res
    }

    suspend fun setFilteringEnabled(enabled: Boolean, interval: Long = 24): Result<String> {
        val res = api.setFilteringConfig(enabled, interval)
        if (res.isSuccess) {
            _filteringStatus.value = _filteringStatus.value?.copy(enabled = enabled, interval = interval)
        }
        return res
    }

    suspend fun saveUserRules(rules: List<String>): Result<String> {
        val res = api.setUserRules(rules)
        if (res.isSuccess) {
            _filteringStatus.value = _filteringStatus.value?.copy(userRules = rules)
        }
        return res
    }

    suspend fun setSafeBrowsing(enabled: Boolean): Result<String> {
        val res = api.setSafeBrowsing(enabled)
        if (res.isSuccess) {
            _safeBrowsingEnabled.value = enabled
        }
        return res
    }

    suspend fun setParental(enabled: Boolean): Result<String> {
        val res = api.setParental(enabled)
        if (res.isSuccess) {
            _parentalEnabled.value = enabled
        }
        return res
    }

    suspend fun setSafeSearch(enabled: Boolean): Result<String> {
        val res = api.setSafeSearch(enabled)
        if (res.isSuccess) {
            _safeSearchEnabled.value = enabled
        }
        return res
    }

    suspend fun saveRebindConfig(enabled: Boolean, strictMode: Boolean, whitelistedDomains: List<String>): Result<RebindConfig> {
        val res = api.setRebindConfig(enabled, strictMode, whitelistedDomains)
        res.onSuccess { _rebindConfig.value = it }
        return res
    }

    suspend fun saveODoHConfig(enabled: Boolean, preset: String, relayUrl: String, targetUrl: String): Result<ODoHConfig> {
        val res = api.setODoHConfig(enabled, preset, relayUrl, targetUrl)
        res.onSuccess { _odohConfig.value = it }
        return res
    }

    suspend fun saveTelegramConfig(enabled: Boolean, token: String, chatId: String): Result<TelegramConfig> {
        val res = api.setTelegramConfig(enabled, token, chatId)
        res.onSuccess { _telegramConfig.value = it }
        return res
    }

    suspend fun toggleBlockedService(serviceName: String, block: Boolean): Result<String> {
        val current = _blockedServices.value.toMutableList()
        if (block) {
            if (!current.contains(serviceName)) current.add(serviceName)
        } else {
            current.remove(serviceName)
        }
        val res = api.setBlockedServices(current)
        if (res.isSuccess) {
            _blockedServices.value = current
        }
        return res
    }

    suspend fun setGameMode(enabled: Boolean): Result<GameModeStatus> {
        val res = api.setGameModeConfig(enabled)
        res.onSuccess { _gameModeStatus.value = it }
        return res
    }

    suspend fun refreshQueryLogs(limit: Int = 100) {
        _isRefreshing.value = true
        val result = api.getQueryLog(limit)
        result.onSuccess { _queryLogs.value = it }
            .onFailure { _errorMessage.value = "Failed to fetch logs: ${it.localizedMessage}" }
        _isRefreshing.value = false
    }

    suspend fun refreshDevices() {
        _isRefreshing.value = true
        val result = api.getDetectedDevices()
        result.onSuccess { _detectedDevices.value = it }
            .onFailure { _errorMessage.value = "Failed to fetch devices: ${it.localizedMessage}" }
        _isRefreshing.value = false
    }

    suspend fun refreshDohInfo() {
        val result = api.getDohInfo()
        result.onSuccess { _dohInfo.value = it }
    }

    suspend fun toggleProtection(enabled: Boolean): Result<String> {
        val res = api.setProtection(enabled)
        if (res.isSuccess) {
            refreshDashboard()
        }
        return res
    }

    suspend fun toggleGameMode(): Result<GameModeStatus> {
        val res = api.toggleGameMode()
        res.onSuccess { _gameModeStatus.value = it }
        return res
    }

    suspend fun runMaintenance(): Result<MaintenanceResult> {
        return api.runMaintenance()
    }

    suspend fun unblockDomain(domain: String): Result<String> {
        return api.unblockDomain(domain)
    }

    suspend fun blockDomain(domain: String): Result<String> {
        return api.blockDomain(domain)
    }
}
