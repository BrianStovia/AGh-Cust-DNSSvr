package com.brst.dns.data.api

import com.brst.dns.data.preferences.AppPreferences

class AdGuardHomeApi(
    private val preferences: AppPreferences,
    private val client: ApiClient = ApiClient()
) {

    private val baseUrl: String get() = preferences.serverUrl.value
    private val user: String get() = preferences.username.value
    private val pass: String get() = preferences.password.value

    suspend fun getStatus(): Result<ServerStatus> {
        return client.get(baseUrl, "control/status", user, pass, ServerStatus::class.java)
    }

    suspend fun getStats(): Result<ServerStats> {
        return client.get(baseUrl, "control/stats", user, pass, ServerStats::class.java)
    }

    suspend fun getQueryLog(limit: Int = 100): Result<QueryLogResponse> {
        return client.get(baseUrl, "control/querylog?limit=$limit", user, pass, QueryLogResponse::class.java)
    }

    suspend fun setProtection(enabled: Boolean): Result<String> {
        val payload = mapOf("enabled" to enabled)
        return client.postRaw(baseUrl, "control/dns_config", user, pass, payload)
    }

    suspend fun getGameModeStatus(): Result<GameModeStatus> {
        return client.get(baseUrl, "control/gamemode/status", user, pass, GameModeStatus::class.java)
    }

    suspend fun toggleGameMode(): Result<GameModeStatus> {
        return client.post(baseUrl, "control/gamemode/toggle", user, pass, null, GameModeStatus::class.java)
    }

    suspend fun runMaintenance(): Result<MaintenanceResult> {
        return client.post(baseUrl, "control/maintenance/optimize", user, pass, null, MaintenanceResult::class.java)
    }

    suspend fun getDetectedDevices(): Result<DetectedDevicesResponse> {
        return client.get(baseUrl, "control/devices/detected", user, pass, DetectedDevicesResponse::class.java)
    }

    suspend fun getDohInfo(): Result<DohInfoResponse> {
        return client.get(baseUrl, "control/doh/info", user, pass, DohInfoResponse::class.java)
    }

    suspend fun unblockDomain(domain: String): Result<String> {
        val rule = "@@||$domain^"
        val payload = mapOf("rules" to listOf(rule))
        return client.postRaw(baseUrl, "control/filtering/set_rules", user, pass, payload)
    }

    suspend fun blockDomain(domain: String): Result<String> {
        val rule = "||$domain^"
        val payload = mapOf("rules" to listOf(rule))
        return client.postRaw(baseUrl, "control/filtering/set_rules", user, pass, payload)
    }
}
