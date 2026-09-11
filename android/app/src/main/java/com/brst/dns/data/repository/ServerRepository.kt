package com.brst.dns.data.repository

import com.brst.dns.data.api.AdGuardHomeApi
import com.brst.dns.data.api.DetectedDevicesResponse
import com.brst.dns.data.api.DohInfoResponse
import com.brst.dns.data.api.GameModeStatus
import com.brst.dns.data.api.MaintenanceResult
import com.brst.dns.data.api.QueryLogResponse
import com.brst.dns.data.api.ServerStats
import com.brst.dns.data.api.ServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServerRepository(private val api: AdGuardHomeApi) {

    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus: StateFlow<ServerStatus?> = _serverStatus.asStateFlow()

    private val _serverStats = MutableStateFlow<ServerStats?>(null)
    val serverStats: StateFlow<ServerStats?> = _serverStats.asStateFlow()

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

        val statusResult = api.getStatus()
        statusResult.onSuccess { _serverStatus.value = it }
            .onFailure { _errorMessage.value = "Failed to fetch status: ${it.localizedMessage}" }

        val statsResult = api.getStats()
        statsResult.onSuccess { _serverStats.value = it }

        val gameModeResult = api.getGameModeStatus()
        gameModeResult.onSuccess { _gameModeStatus.value = it }

        _isRefreshing.value = false
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
