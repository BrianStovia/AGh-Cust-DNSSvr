package com.brst.dns.doh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class DohDnsPacketProcessor(
    private val dohUrl: String,
    private val outStream: FileOutputStream,
    private val scope: CoroutineScope
) {

    private val dnsMediaType = "application/dns-message".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private val _totalQueries = MutableStateFlow(0L)
    val totalQueries: StateFlow<Long> = _totalQueries.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow(0L)
    val lastLatencyMs: StateFlow<Long> = _lastLatencyMs.asStateFlow()

    fun processPacket(packetData: ByteArray, length: Int) {
        if (length < 28) return // Minimum IPv4 (20) + UDP (8) header length

        val buffer = ByteBuffer.wrap(packetData, 0, length)
        val versionAndIHL = buffer.get(0).toInt() and 0xFF
        val version = versionAndIHL shr 4
        if (version != 4) return // Only process IPv4 for local TUN DNS

        val ihl = (versionAndIHL and 0x0F) * 4
        val protocol = buffer.get(9).toInt() and 0xFF
        if (protocol != 17) return // Protocol must be UDP (17)

        val srcIp = ByteArray(4).apply { buffer.position(12); buffer.get(this) }
        val dstIp = ByteArray(4).apply { buffer.position(16); buffer.get(this) }

        buffer.position(ihl)
        val srcPort = buffer.short.toInt() and 0xFFFF
        val dstPort = buffer.short.toInt() and 0xFFFF
        val udpLength = buffer.short.toInt() and 0xFFFF
        buffer.short // Skip checksum

        if (dstPort != 53 && srcPort != 53) return // Must be DNS port 53

        val dnsPayloadLength = udpLength - 8
        if (dnsPayloadLength <= 0 || (ihl + 8 + dnsPayloadLength) > length) return

        val dnsQueryData = ByteArray(dnsPayloadLength)
        buffer.position(ihl + 8)
        buffer.get(dnsQueryData)

        scope.launch(Dispatchers.IO) {
            forwardDoH(dnsQueryData, srcIp, dstIp, srcPort, dstPort)
        }
    }

    private fun forwardDoH(
        dnsQuery: ByteArray,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int
    ) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(dohUrl)
                .addHeader("Accept", "application/dns-message")
                .addHeader("Content-Type", "application/dns-message")
                .post(dnsQuery.toRequestBody(dnsMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            _lastLatencyMs.value = latency

            if (response.isSuccessful) {
                val dnsResponseBytes = response.body?.bytes()
                if (dnsResponseBytes != null && dnsResponseBytes.isNotEmpty()) {
                    _totalQueries.value += 1
                    sendDnsResponse(dnsResponseBytes, dstIp, srcIp, dstPort, srcPort)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun sendDnsResponse(
        dnsResponse: ByteArray,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int
    ) {
        val ipHeaderLength = 20
        val udpHeaderLength = 8
        val totalLength = ipHeaderLength + udpHeaderLength + dnsResponse.size

        val responseBuffer = ByteBuffer.allocate(totalLength)

        // 1. IPv4 Header
        responseBuffer.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
        responseBuffer.put(0x00.toByte()) // DSCP/ECN
        responseBuffer.putShort(totalLength.toShort()) // Total Length
        responseBuffer.putShort((System.currentTimeMillis() and 0xFFFF).toShort()) // Identification
        responseBuffer.putShort(0x0000.toShort()) // Flags & Fragment Offset
        responseBuffer.put(64.toByte()) // TTL
        responseBuffer.put(17.toByte()) // Protocol UDP (17)
        responseBuffer.putShort(0.toShort()) // Checksum placeholder
        responseBuffer.put(srcIp) // Source IP
        responseBuffer.put(dstIp) // Destination IP

        // Calculate IP Checksum
        val ipChecksum = computeChecksum(responseBuffer.array(), 0, ipHeaderLength)
        responseBuffer.putShort(10, ipChecksum.toShort())

        // 2. UDP Header
        responseBuffer.position(ipHeaderLength)
        responseBuffer.putShort(srcPort.toShort())
        responseBuffer.putShort(dstPort.toShort())
        responseBuffer.putShort((udpHeaderLength + dnsResponse.size).toShort())
        responseBuffer.putShort(0.toShort()) // UDP Checksum (0 is allowed in IPv4)

        // 3. DNS Payload
        responseBuffer.put(dnsResponse)

        try {
            outStream.write(responseBuffer.array(), 0, totalLength)
            outStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            val b1 = data[i].toInt() and 0xFF
            val b2 = data[i + 1].toInt() and 0xFF
            sum += (b1 shl 8) or b2
            i += 2
        }
        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv()) and 0xFFFF
    }
}
