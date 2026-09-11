package com.brst.dns.data.blocklist

import android.content.Context
import com.brst.dns.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class LocalBlocklistManager private constructor(private val context: Context) {

    private val preferences = AppPreferences(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()

    private val _ruleCount = MutableStateFlow(0)
    val ruleCount: StateFlow<Int> = _ruleCount.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _blockedQueriesCount = MutableStateFlow(0L)
    val blockedQueriesCount: StateFlow<Long> = _blockedQueriesCount.asStateFlow()

    init {
        loadInitialRules()
    }

    fun isBlocked(domain: String): Boolean {
        if (!preferences.isBlocklistEnabled.value) return false
        val clean = domain.lowercase().trim().trimEnd('.')
        if (clean.isEmpty()) return false

        // Exact match
        if (blockedDomains.contains(clean)) return true

        // Subdomain traversal match (e.g. ad.doubleclick.net -> doubleclick.net)
        var parent = clean
        while (parent.contains('.')) {
            parent = parent.substringAfter('.')
            if (blockedDomains.contains(parent)) {
                return true
            }
        }

        return false
    }

    fun incrementBlockedCount() {
        _blockedQueriesCount.value += 1
    }

    fun reloadRules() {
        loadInitialRules()
    }

    private fun loadInitialRules() {
        val set = HashSet<String>(10000)

        // 1. Built-in high impact ad & tracker list
        set.addAll(BUILTIN_AD_DOMAINS)

        // 2. Load from disk cache if exists
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                cacheFile.forEachLine { line ->
                    parseRule(line)?.let { set.add(it) }
                }
            }
        } catch (_: Exception) {}

        // 3. Custom user rules from preferences
        val custom = preferences.customBlockedRules.value
        if (custom.isNotBlank()) {
            custom.lines().forEach { line ->
                parseRule(line)?.let { set.add(it) }
            }
        }

        blockedDomains.clear()
        blockedDomains.addAll(set)
        _ruleCount.value = blockedDomains.size
    }

    suspend fun updateFromUrl(url: String): Result<Int> = withContext(Dispatchers.IO) {
        _isUpdating.value = true
        try {
            val targetUrl = url.trim()
            if (targetUrl.isBlank() || !targetUrl.startsWith("http")) {
                _isUpdating.value = false
                return@withContext Result.failure(IllegalArgumentException("URL daftar blokir tidak valid"))
            }

            val request = Request.Builder().url(targetUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                _isUpdating.value = false
                return@withContext Result.failure(Exception("Gagal mengunduh daftar blokir: HTTP ${response.code}"))
            }

            val content = response.body?.string() ?: ""
            val parsedSet = HashSet<String>(50000)

            content.lineSequence().forEach { line ->
                parseRule(line)?.let { parsedSet.add(it) }
            }

            // Save to cache file
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            cacheFile.writeText(content)

            // Save URL to preferences
            preferences.setBlocklistUrl(targetUrl)

            // Re-merge with built-in and custom
            parsedSet.addAll(BUILTIN_AD_DOMAINS)
            val custom = preferences.customBlockedRules.value
            if (custom.isNotBlank()) {
                custom.lines().forEach { line ->
                    parseRule(line)?.let { parsedSet.add(it) }
                }
            }

            blockedDomains.clear()
            blockedDomains.addAll(parsedSet)
            _ruleCount.value = blockedDomains.size

            _isUpdating.value = false
            Result.success(blockedDomains.size)
        } catch (e: Exception) {
            _isUpdating.value = false
            Result.failure(e)
        }
    }

    private fun parseRule(raw: String): String? {
        var line = raw.trim()
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!") || line.startsWith(";")) {
            return null
        }

        // AdGuard / uBlock format: ||domain.com^
        if (line.startsWith("||")) {
            line = line.substring(2)
            if (line.contains("^")) {
                line = line.substringBefore("^")
            }
            if (line.contains("$")) {
                line = line.substringBefore("$")
            }
            line = line.trim().lowercase().trimEnd('.')
            return if (line.isNotEmpty() && !line.contains("/")) line else null
        }

        // Hosts file format: 0.0.0.0 domain.com or 127.0.0.1 domain.com
        if (line.startsWith("0.0.0.0") || line.startsWith("127.0.0.1")) {
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val candidate = parts[1].trim().lowercase().trimEnd('.')
                if (candidate.isNotEmpty() && candidate != "localhost" && candidate != "broadcasthost") {
                    return candidate
                }
            }
            return null
        }

        // Plain domain line
        val candidate = line.substringBefore("#").trim().lowercase().trimEnd('.')
        return if (candidate.isNotEmpty() && !candidate.contains(" ") && !candidate.contains("/")) candidate else null
    }

    companion object {
        private const val CACHE_FILE_NAME = "blocklist_cache.txt"

        @Volatile
        private var instance: LocalBlocklistManager? = null

        fun getInstance(context: Context): LocalBlocklistManager {
            return instance ?: synchronized(this) {
                instance ?: LocalBlocklistManager(context.applicationContext).also { instance = it }
            }
        }

        // Top embedded ad, telemetry, and tracking networks
        private val BUILTIN_AD_DOMAINS = listOf(
            // Google Ads & Tracking
            "doubleclick.net",
            "googleads.g.doubleclick.net",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "pagead2.googleadservices.com",
            "www.googleadservices.com",
            "googleadservices.com",
            "ads.google.com",
            "analytics.google.com",
            "google-analytics.com",
            "ssl.google-analytics.com",
            "googletagmanager.com",
            "googletagservices.com",
            "crashlytics.com",

            // Meta / Facebook Tracking
            "pixel.facebook.com",
            "an.facebook.com",
            "ads.facebook.com",
            "graph.facebook.com",
            "analytics.facebook.com",

            // TikTok & ByteDance
            "ads.tiktok.com",
            "analytics.tiktok.com",
            "log.byteoversea.com",
            "mon.musical.ly",
            "sf-tb-sg.ibytedtos.com",

            // Mobile In-App Ad Networks
            "unityads.unity3d.com",
            "ads.api.vungle.com",
            "vungle.com",
            "applovin.com",
            "d.applovin.com",
            "a.applovin.com",
            "ironsrc.com",
            "adcolony.com",
            "chartboost.com",
            "inmobi.com",
            "mopub.com",
            "appsflyer.com",
            "app.appsflyer.com",
            "adjust.com",
            "app.adjust.com",
            "branch.io",
            "api2.branch.io",
            "kochava.com",
            "control.kochava.com",
            "singular.net",
            "tapjoy.com",
            "fyber.com",
            "mintegral.com",
            "criteo.com",
            "adnxs.com",
            "taboola.com",
            "outbrain.com",
            "popads.net",
            "propellerads.com",
            "exoclick.com",
            "trafficjunky.com",
            "adsterra.com",
            "bidswitch.net",
            "rubiconproject.com",
            "openx.net",
            "pubmatic.com",
            "smartadserver.com",
            "scorecardresearch.com",
            "quantserve.com",
            "statcounter.com",
            "hotjar.com",
            "clarity.ms",
            "telemetry.microsoft.com",
            "vortex.data.microsoft.com",
            "telemetry.urs.microsoft.com",
            "ad.toutiao.com",
            "iads.apple.com",
            "metrics.apple.com"
        )
    }
}
