package com.brst.dns.data.api

import com.google.gson.annotations.SerializedName

// --- Server Status ---
data class ServerStatus(
    @SerializedName("protection_enabled") val protectionEnabled: Boolean = true,
    @SerializedName("running") val running: Boolean = true,
    @SerializedName("version") val version: String = "",
    @SerializedName("language") val language: String = "en",
    @SerializedName("dns_addresses") val dnsAddresses: List<String> = emptyList(),
    @SerializedName("dns_port") val dnsPort: Int = 53,
    @SerializedName("http_port") val httpPort: Int = 80,
    @SerializedName("start_time") val startTime: Long = 0L
)

// --- Server Statistics ---
data class ServerStats(
    @SerializedName("num_dns_queries") val numDnsQueries: Long = 0L,
    @SerializedName("num_blocked_filtering") val numBlockedFiltering: Long = 0L,
    @SerializedName("num_replaced_safebrowsing") val numReplacedSafebrowsing: Long = 0L,
    @SerializedName("num_replaced_safesearch") val numReplacedSafesearch: Long = 0L,
    @SerializedName("avg_processing_time") val avgProcessingTime: Double = 0.0,
    @SerializedName("top_queried_domains") val topQueriedDomains: List<Map<String, Long>> = emptyList(),
    @SerializedName("top_blocked_domains") val topBlockedDomains: List<Map<String, Long>> = emptyList(),
    @SerializedName("top_clients") val topClients: List<Map<String, Long>> = emptyList(),
    @SerializedName("top_upstreams_responses") val topUpstreamsResponses: List<Map<String, Long>> = emptyList()
) {
    val blockPercentage: Double
        get() = if (numDnsQueries > 0) {
            (numBlockedFiltering.toDouble() / numDnsQueries.toDouble()) * 100.0
        } else {
            0.0
        }
}

// --- Query Log ---
data class QueryLogResponse(
    @SerializedName("data") val data: List<QueryLogItem> = emptyList(),
    @SerializedName("oldest") val oldest: String = ""
)

data class QueryLogItem(
    @SerializedName("time") val time: String = "",
    @SerializedName("client") val client: String = "",
    @SerializedName("client_proto") val clientProto: String = "",
    @SerializedName("elapsed_ms") val elapsedMs: String = "0",
    @SerializedName("reason") val reason: String = "NotFilteredNotFound",
    @SerializedName("status") val status: String = "NOERROR",
    @SerializedName("question") val question: QuestionDetails? = null,
    @SerializedName("rules") val rules: List<RuleDetails> = emptyList(),
    @SerializedName("upstream") val upstream: String = ""
) {
    val isBlocked: Boolean
        get() = reason.contains("Filtered", ignoreCase = true) ||
                reason.contains("Block", ignoreCase = true) ||
                reason.contains("Parental", ignoreCase = true) ||
                reason.contains("SafeBrowsing", ignoreCase = true)
}

data class QuestionDetails(
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String = "A",
    @SerializedName("class") val qClass: String = "IN"
)

data class RuleDetails(
    @SerializedName("text") val text: String = "",
    @SerializedName("filter_list_id") val filterListId: Long = 0L
)

// --- DNS General Config (/control/dns_info & /control/dns_config) ---
data class DnsConfig(
    @SerializedName("upstream_dns") val upstreamDns: List<String> = listOf("https://dns.quad9.net/dns-query", "tls://1.1.1.1"),
    @SerializedName("fallback_dns") val fallbackDns: List<String> = emptyList(),
    @SerializedName("bootstrap_dns") val bootstrapDns: List<String> = listOf("9.9.9.9", "1.1.1.1"),
    @SerializedName("upstream_mode") val upstreamMode: String = "load_balance",
    @SerializedName("edns_cs_enabled") val ednsCsEnabled: Boolean = false,
    @SerializedName("dnssec_enabled") val dnssecEnabled: Boolean = false,
    @SerializedName("disable_ipv6") val disableIpv6: Boolean = false,
    @SerializedName("ratelimit") val ratelimit: Int = 20,
    @SerializedName("cache_size") val cacheSize: Long = 4194304L,
    @SerializedName("cache_ttl_min") val cacheTtlMin: Long = 0L,
    @SerializedName("cache_ttl_max") val cacheTtlMax: Long = 0L,
    @SerializedName("cache_optimistic") val cacheOptimistic: Boolean = false,
    @SerializedName("blocking_mode") val blockingMode: String = "default"
)

// --- Filtering Config (/control/filtering/status & /control/filtering/config) ---
data class FilteringStatus(
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("interval") val interval: Long = 24L,
    @SerializedName("user_rules") val userRules: List<String> = emptyList()
)

// --- Protection Toggles ---
data class BooleanStatus(
    @SerializedName("enabled") val enabled: Boolean = false
)

// --- Anti-DNS Rebinding Config (/control/rebind/status & /control/rebind/config) ---
data class RebindConfig(
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("strict_mode") val strictMode: Boolean = false,
    @SerializedName("whitelisted_domains") val whitelistedDomains: List<String> = emptyList(),
    @SerializedName("blocked_count") val blockedCount: Long = 0L
)

// --- Oblivious DoH (ODoH) Config (/control/odoh/status & /control/odoh/configure) ---
data class ODoHConfig(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("preset") val preset: String = "cloudflare",
    @SerializedName("relay_url") val relayUrl: String = "https://odoh-relay.cloudflare.com/proxy",
    @SerializedName("target_url") val targetUrl: String = "https://odoh.cloudflare-dns.com/dns-query",
    @SerializedName("anonymity_score") val anonymityScore: String = ""
)

// --- Telegram Bot Config (/control/telegram/status & /control/telegram/config) ---
data class TelegramConfig(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("token") val token: String = "",
    @SerializedName("admin_chat_id") val adminChatId: String = "",
    @SerializedName("status") val status: String = "disconnected"
)

// --- Blocked Services (/control/blocked_services/list & /control/blocked_services/set) ---
data class BlockedServicesList(
    @SerializedName("blocked_services") val blockedServices: List<String> = emptyList()
)

// --- Smart Game Mode QoS ---
data class GameModeStatus(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("mode") val mode: String = "Low-Latency QoS",
    @SerializedName("active_rules_count") val activeRulesCount: Int = 0,
    @SerializedName("packet_prioritization") val packetPrioritization: Boolean = true
)

// --- Auto-Maintenance & RAM Compactor ---
data class MaintenanceResult(
    @SerializedName("status") val status: String = "ok",
    @SerializedName("freed_memory_mb") val freedMemoryMb: Double = 0.0,
    @SerializedName("allocated_mb") val allocatedMb: Double = 0.0,
    @SerializedName("sys_memory_mb") val sysMemoryMb: Double = 0.0,
    @SerializedName("num_gc") val numGc: Long = 0L,
    @SerializedName("timestamp") val timestamp: String = ""
)

// --- Discovered Devices via ARP/DHCP ---
data class DetectedDevicesResponse(
    @SerializedName("total_devices") val totalDevices: Int = 0,
    @SerializedName("devices") val devices: List<DetectedDevice> = emptyList()
)

data class DetectedDevice(
    @SerializedName("ip") val ip: String = "",
    @SerializedName("mac") val mac: String = "",
    @SerializedName("device_type") val deviceType: String = "unknown",
    @SerializedName("vendor") val vendor: String = "Generic Device",
    @SerializedName("hostname") val hostname: String = "",
    @SerializedName("last_seen") val lastSeen: Long = 0L
)

// --- DoH Hub Info ---
data class DohInfoResponse(
    @SerializedName("host") val host: String = "",
    @SerializedName("doh_url") val dohUrl: String = "",
    @SerializedName("doh_client_template") val dohClientTemplate: String = "",
    @SerializedName("dot_url") val dotUrl: String = "",
    @SerializedName("doq_url") val doqUrl: String = "",
    @SerializedName("tls_active") val tlsActive: Boolean = false,
    @SerializedName("status") val status: String = "ready"
)
