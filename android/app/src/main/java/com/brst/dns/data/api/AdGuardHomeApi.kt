package com.brst.dns.data.api

import com.brst.dns.data.preferences.AppPreferences

class AdGuardHomeApi(
    private val preferences: AppPreferences,
    private val client: ApiClient = ApiClient()
) {

    private val baseUrl: String get() = preferences.serverUrl.value
    private val authParams: AuthParams
        get() = AuthParams(
            authType = preferences.authType.value,
            apiKey = preferences.apiKey.value,
            username = preferences.username.value,
            password = preferences.password.value
        )

    suspend fun getStatus(): Result<ServerStatus> {
        return client.get(baseUrl, "control/status", authParams, ServerStatus::class.java)
    }

    suspend fun getStats(): Result<ServerStats> {
        return client.get(baseUrl, "control/stats", authParams, ServerStats::class.java)
    }

    suspend fun getQueryLog(limit: Int = 100): Result<QueryLogResponse> {
        return client.get(baseUrl, "control/querylog?limit=$limit", authParams, QueryLogResponse::class.java)
    }

    suspend fun setProtection(enabled: Boolean): Result<String> {
        val payload = mapOf("enabled" to enabled)
        return client.postRaw(baseUrl, "control/dns_config", authParams, payload)
    }

    suspend fun getGameModeStatus(): Result<GameModeStatus> {
        return client.get(baseUrl, "control/gamemode/status", authParams, GameModeStatus::class.java)
    }

    suspend fun toggleGameMode(): Result<GameModeStatus> {
        return client.post(baseUrl, "control/gamemode/toggle", authParams, null, GameModeStatus::class.java)
    }

    suspend fun runMaintenance(): Result<MaintenanceResult> {
        return client.post(baseUrl, "control/maintenance/optimize", authParams, null, MaintenanceResult::class.java)
    }

    suspend fun getDetectedDevices(): Result<DetectedDevicesResponse> {
        return client.get(baseUrl, "control/devices/detected", authParams, DetectedDevicesResponse::class.java)
    }

    suspend fun getDohInfo(): Result<DohInfoResponse> {
        return client.get(baseUrl, "control/doh/info", authParams, DohInfoResponse::class.java)
    }

    suspend fun unblockDomain(domain: String): Result<String> {
        val rule = "@@||$domain^"
        val payload = mapOf("rules" to listOf(rule))
        return client.postRaw(baseUrl, "control/filtering/set_rules", authParams, payload)
    }

    suspend fun blockDomain(domain: String): Result<String> {
        val rule = "||$domain^"
        val payload = mapOf("rules" to listOf(rule))
        return client.postRaw(baseUrl, "control/filtering/set_rules", authParams, payload)
    }
}
