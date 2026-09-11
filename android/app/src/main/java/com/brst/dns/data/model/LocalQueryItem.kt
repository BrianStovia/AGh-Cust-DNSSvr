package com.brst.dns.data.model

data class LocalQueryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val domain: String,
    val queryType: String = "A",
    val protocol: String = "DoT (TLS)",
    val latencyMs: Long,
    val timestamp: String,
    val status: String = "ENCRYPTED", // "ENCRYPTED" or "BLOCKED"
    val success: Boolean = true,
    val blocked: Boolean = false
)
