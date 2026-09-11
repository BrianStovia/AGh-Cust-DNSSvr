package com.brst.dns.data.api

import com.brst.dns.data.preferences.AppPreferences

class AdGuardHomeApi(
    private val preferences: AppPreferences,
    private val client: ApiClient = ApiClient()
) {

    private val baseUrl: String get() = preferences.serverUrl.value
    private val user: String get() = preferences.username.value
    private val pass: String get() = preferences.password.value

    // --- Core Status & Stats ---
    suspend fun getStatus(): Result<ServerStatus> {
        return client.get(baseUrl, "control/status", user, pass, ServerStatus::class.java)
    }

    suspend fun getStats(): Result<ServerStats> {
        return client.get(baseUrl, "control/stats", user, pass, ServerStats::class.java)
    }

    suspend fun setProtection(enabled: Boolean): Result<String> {
        val payload = mapOf("enabled" to enabled)
        return client.postRaw(baseUrl, "control/dns_config", user, pass, payload)
    }

    // --- DNS Upstream & General Settings ---
    suspend fun getDnsConfig(): Result<DnsConfig> {
        return client.get(baseUrl, "control/dns_info", user, pass, DnsConfig::class.java)
    }

    suspend fun setDnsConfig(config: DnsConfig): Result<String> {
        return client.postRaw(baseUrl, "control/dns_config", user, pass, config)
    }

    // --- Filtering & Safe Search ---
    suspend fun getFilteringStatus(): Result<FilteringStatus> {
        return client.get(baseUrl, "control/filtering/status", user, pass, FilteringStatus::class.java)
    }

    suspend fun setFilteringConfig(enabled: Boolean, interval: Long): Result<String> {
        val payload = mapOf("enabled" to enabled, "interval" to interval)
        return client.postRaw(baseUrl, "control/filtering/config", user, pass, payload)
    }

    suspend fun setUserRules(rules: List<String>): Result<String> {
        val payload = mapOf("rules" to rules)
        return client.postRaw(baseUrl, "control/filtering/set_rules", user, pass, payload)
    }

    suspend fun getSafeBrowsingStatus(): Result<BooleanStatus> {
        return client.get(baseUrl, "control/safebrowsing/status", user, pass, BooleanStatus::class.java)
    }

    suspend fun setSafeBrowsing(enabled: Boolean): Result<String> {
        val path = if (enabled) "control/safebrowsing/enable" else "control/safebrowsing/disable"
        return client.postRaw(baseUrl, path, user, pass, null)
    }

    suspend fun getParentalStatus(): Result<BooleanStatus> {
        return client.get(baseUrl, "control/parental/status", user, pass, BooleanStatus::class.java)
    }

    suspend fun setParental(enabled: Boolean): Result<String> {
        val path = if (enabled) "control/parental/enable" else "control/parental/disable"
        return client.postRaw(baseUrl, path, user, pass, null)
    }

    suspend fun getSafeSearchStatus(): Result<BooleanStatus> {
        return client.get(baseUrl, "control/safesearch/status", user, pass, BooleanStatus::class.java)
    }

    suspend fun setSafeSearch(enabled: Boolean): Result<String> {
        val path = if (enabled) "control/safesearch/enable" else "control/safesearch/disable"
        return client.postRaw(baseUrl, path, user, pass, null)
    }

    // --- Anti-DNS Rebinding Shield ---
    suspend fun getRebindConfig(): Result<RebindConfig> {
        return client.get(baseUrl, "control/rebind/status", user, pass, RebindConfig::class.java)
    }

    suspend fun setRebindConfig(enabled: Boolean, strictMode: Boolean, whitelistedDomains: List<String>): Result<RebindConfig> {
        val payload = mapOf(
            "enabled" to enabled,
            "strict_mode" to strictMode,
            "whitelisted_domains" to whitelistedDomains
        )
        return client.post(baseUrl, "control/rebind/config", user, pass, payload, RebindConfig::class.java)
    }

    // --- Oblivious DoH (ODoH) Shield ---
    suspend fun getODoHConfig(): Result<ODoHConfig> {
        return client.get(baseUrl, "control/odoh/status", user, pass, ODoHConfig::class.java)
    }

    suspend fun setODoHConfig(enabled: Boolean, preset: String, relayUrl: String, targetUrl: String): Result<ODoHConfig> {
        val payload = mapOf(
            "enabled" to enabled,
            "preset" to preset,
            "relay_url" to relayUrl,
            "target_url" to targetUrl
        )
        return client.post(baseUrl, "control/odoh/configure", user, pass, payload, ODoHConfig::class.java)
    }

    // --- Telegram Bot ---
    suspend fun getTelegramConfig(): Result<TelegramConfig> {
        return client.get(baseUrl, "control/telegram/status", user, pass, TelegramConfig::class.java)
    }

    suspend fun setTelegramConfig(enabled: Boolean, token: String, chatId: String): Result<TelegramConfig> {
        val payload = mapOf(
            "enabled" to enabled,
            "token" to token,
            "admin_chat_id" to chatId
        )
        return client.post(baseUrl, "control/telegram/config", user, pass, payload, TelegramConfig::class.java)
    }

    // --- Blocked Services ---
    suspend fun getBlockedServices(): Result<BlockedServicesList> {
        return client.get(baseUrl, "control/blocked_services/list", user, pass, BlockedServicesList::class.java)
    }

    suspend fun setBlockedServices(services: List<String>): Result<String> {
        val payload = mapOf("blocked_services" to services)
        return client.postRaw(baseUrl, "control/blocked_services/set", user, pass, payload)
    }

    // --- Smart Game Mode QoS ---
    suspend fun getGameModeStatus(): Result<GameModeStatus> {
        return client.get(baseUrl, "control/gamemode/status", user, pass, GameModeStatus::class.java)
    }

    suspend fun setGameModeConfig(enabled: Boolean): Result<GameModeStatus> {
        val payload = mapOf("enabled" to enabled)
        return client.post(baseUrl, "control/gamemode/config", user, pass, payload, GameModeStatus::class.java)
    }

    suspend fun toggleGameMode(): Result<GameModeStatus> {
        return client.post(baseUrl, "control/gamemode/toggle", user, pass, null, GameModeStatus::class.java)
    }

    // --- Query Logs & Devices & Maintenance ---
    suspend fun getQueryLog(limit: Int = 100): Result<QueryLogResponse> {
        return client.get(baseUrl, "control/querylog?limit=$limit", user, pass, QueryLogResponse::class.java)
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
