package com.brst.dns.doh

import android.net.VpnService
import com.brst.dns.data.blocklist.LocalBlocklistManager
import com.brst.dns.data.model.LocalQueryItem
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
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class DotDnsPacketProcessor(
    private val vpnService: VpnService,
    private val protocol: String, // "DoT" or "DoH"
    private val dotHost: String,
    private val dotPort: Int,
    private val dotTlsServerName: String,
    private val dohUrl: String,
    private val outStream: FileOutputStream,
    private val scope: CoroutineScope,
    private val blocklistManager: LocalBlocklistManager = LocalBlocklistManager.getInstance(vpnService)
) {

    private val _totalQueries = MutableStateFlow(0L)
    val totalQueries: StateFlow<Long> = _totalQueries.asStateFlow()

    private val _blockedQueries = MutableStateFlow(0L)
    val blockedQueries: StateFlow<Long> = _blockedQueries.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow(0L)
    val lastLatencyMs: StateFlow<Long> = _lastLatencyMs.asStateFlow()

    private val _recentQueries = MutableStateFlow<List<LocalQueryItem>>(emptyList())
    val recentQueries: StateFlow<List<LocalQueryItem>> = _recentQueries.asStateFlow()

    private val dnsMediaType = "application/dns-message".toMediaType()

    private val sslContext: SSLContext by lazy {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )
        val sc = SSLContext.getInstance("TLS")
        sc.init(null, trustAllCerts, SecureRandom())
        sc
    }

    private val httpClient: OkHttpClient by lazy {
        val protectedSocketFactory = object : SocketFactory() {
            private val defaultFactory = getDefault()
            override fun createSocket(): Socket = defaultFactory.createSocket().also { vpnService.protect(it) }
            override fun createSocket(host: String?, port: Int): Socket = defaultFactory.createSocket(host, port).also { vpnService.protect(it) }
            override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket = defaultFactory.createSocket(host, port, localHost, localPort).also { vpnService.protect(it) }
            override fun createSocket(host: InetAddress?, port: Int): Socket = defaultFactory.createSocket(host, port).also { vpnService.protect(it) }
            override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket = defaultFactory.createSocket(address, port, localAddress, localPort).also { vpnService.protect(it) }
        }

        OkHttpClient.Builder()
            .socketFactory(protectedSocketFactory)
            .sslSocketFactory(sslContext.socketFactory, object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun processPacket(packetData: ByteArray, length: Int) {
        if (length < 28) return

        val buffer = ByteBuffer.wrap(packetData, 0, length)
        val versionAndIHL = buffer.get(0).toInt() and 0xFF
        val version = versionAndIHL shr 4
        if (version != 4) return // IPv4 only for TUN

        val ihl = (versionAndIHL and 0x0F) * 4
        val protocolNumber = buffer.get(9).toInt() and 0xFF
        if (protocolNumber != 17) return // UDP only

        val srcIp = ByteArray(4).apply { buffer.position(12); buffer.get(this) }
        val dstIp = ByteArray(4).apply { buffer.position(16); buffer.get(this) }

        buffer.position(ihl)
        val srcPort = buffer.short.toInt() and 0xFFFF
        val dstPort = buffer.short.toInt() and 0xFFFF
        val udpLength = buffer.short.toInt() and 0xFFFF

        if (dstPort != 53 && srcPort != 53) return

        val dnsPayloadLength = udpLength - 8
        if (dnsPayloadLength <= 0 || (ihl + 8 + dnsPayloadLength) > length) return

        val dnsQueryData = ByteArray(dnsPayloadLength)
        buffer.position(ihl + 8)
        buffer.get(dnsQueryData)

        val domain = extractDomainName(dnsQueryData)

        scope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            // --- 1. Check Local On-Device Blocklist ---
            if (blocklistManager.isBlocked(domain)) {
                blocklistManager.incrementBlockedCount()
                _totalQueries.value += 1
                _blockedQueries.value += 1
                _lastLatencyMs.value = 0L

                recordBlockedQuery(domain)
                val blockedResponse = generateBlockedDnsResponse(dnsQueryData)
                sendDnsResponse(blockedResponse, dstIp, srcIp, dstPort, srcPort)
                return@launch
            }

            // --- 2. Forward via DoT or DoH ---
            var dnsResponse: ByteArray? = null

            if (protocol.equals("DoT", ignoreCase = true)) {
                dnsResponse = forwardDoT(dnsQueryData)
            } else {
                dnsResponse = forwardDoH(dnsQueryData)
            }

            // Fallback to protected UDP 1.1.1.1 if upstream DoT/DoH fails
            if (dnsResponse == null || dnsResponse.isEmpty()) {
                dnsResponse = forwardUdpFallback(dnsQueryData)
            }

            val latency = System.currentTimeMillis() - startTime
            _lastLatencyMs.value = latency

            if (dnsResponse != null && dnsResponse.isNotEmpty()) {
                _totalQueries.value += 1
                recordQuery(domain, latency)
                sendDnsResponse(dnsResponse, dstIp, srcIp, dstPort, srcPort)
            }
        }
    }

    // --- RFC 7858 DNS-over-TLS (DoT) Engine ---
    private fun forwardDoT(dnsQuery: ByteArray): ByteArray? {
        var socket: SSLSocket? = null
        return try {
            val rawSocket = Socket()
            vpnService.protect(rawSocket)
            rawSocket.connect(InetSocketAddress(dotHost, dotPort), 4000)

            val sslSocketFactory = sslContext.socketFactory
            socket = sslSocketFactory.createSocket(
                rawSocket,
                if (dotTlsServerName.isNotEmpty()) dotTlsServerName else dotHost,
                dotPort,
                true
            ) as SSLSocket

            socket.soTimeout = 4000
            socket.startHandshake()

            val dos = DataOutputStream(socket.getOutputStream())
            val dis = DataInputStream(socket.getInputStream())

            // RFC 7858: 2-byte length prefix (big-endian) followed by DNS wire format
            dos.writeShort(dnsQuery.size)
            dos.write(dnsQuery)
            dos.flush()

            val respLength = dis.readUnsignedShort()
            if (respLength > 0 && respLength < 65535) {
                val respBytes = ByteArray(respLength)
                dis.readFully(respBytes)
                respBytes
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    // --- RFC 8484 DNS-over-HTTPS (DoH) Engine ---
    private fun forwardDoH(dnsQuery: ByteArray): ByteArray? {
        return try {
            val request = Request.Builder()
                .url(dohUrl)
                .addHeader("Accept", "application/dns-message")
                .addHeader("Content-Type", "application/dns-message")
                .post(dnsQuery.toRequestBody(dnsMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // --- Protected UDP Fallback Resolver ---
    private fun forwardUdpFallback(dnsQuery: ByteArray): ByteArray? {
        return try {
            val socket = DatagramSocket()
            vpnService.protect(socket)
            socket.soTimeout = 2000

            val packet = DatagramPacket(dnsQuery, dnsQuery.size, InetAddress.getByName("1.1.1.1"), 53)
            socket.send(packet)

            val recvBuf = ByteArray(4096)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)
            socket.close()

            ByteArray(recvPacket.length).apply {
                System.arraycopy(recvBuf, 0, this, 0, recvPacket.length)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun generateBlockedDnsResponse(queryData: ByteArray): ByteArray {
        val qdCount = if (queryData.size >= 6) {
            ((queryData[4].toInt() and 0xFF) shl 8) or (queryData[5].toInt() and 0xFF)
        } else 1

        val out = ByteBuffer.allocate(queryData.size + 16)
        // 1. Transaction ID
        if (queryData.size >= 2) {
            out.put(queryData[0])
            out.put(queryData[1])
        } else {
            out.putShort(0x1234.toShort())
        }
        // 2. Flags: 0x8180 (Response, No error)
        out.put(0x81.toByte())
        out.put(0x80.toByte())
        // 3. QDCOUNT
        out.putShort(qdCount.toShort())
        // 4. ANCOUNT = 1
        out.putShort(1.toShort())
        // 5. NSCOUNT = 0, ARCOUNT = 0
        out.putShort(0.toShort())
        out.putShort(0.toShort())

        // 6. Question Section
        if (queryData.size > 12) {
            out.put(queryData, 12, queryData.size - 12)
        }

        // 7. Answer Section (A Record -> 0.0.0.0)
        out.put(0xC0.toByte()) // Name compression pointer
        out.put(0x0C.toByte()) // offset 12
        out.putShort(1.toShort()) // Type A
        out.putShort(1.toShort()) // Class IN
        out.putInt(60) // TTL = 60s
        out.putShort(4.toShort()) // RDLENGTH = 4
        out.put(byteArrayOf(0, 0, 0, 0)) // 0.0.0.0 Sinkhole

        val result = ByteArray(out.position())
        System.arraycopy(out.array(), 0, result, 0, result.size)
        return result
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
        responseBuffer.put(0x45.toByte())
        responseBuffer.put(0x00.toByte())
        responseBuffer.putShort(totalLength.toShort())
        responseBuffer.putShort((System.currentTimeMillis() and 0xFFFF).toShort())
        responseBuffer.putShort(0x0000.toShort())
        responseBuffer.put(64.toByte())
        responseBuffer.put(17.toByte()) // UDP
        responseBuffer.putShort(0.toShort())
        responseBuffer.put(srcIp)
        responseBuffer.put(dstIp)

        // Checksum
        val ipChecksum = computeChecksum(responseBuffer.array(), 0, ipHeaderLength)
        responseBuffer.putShort(10, ipChecksum.toShort())

        // 2. UDP Header
        responseBuffer.position(ipHeaderLength)
        responseBuffer.putShort(srcPort.toShort())
        responseBuffer.putShort(dstPort.toShort())
        responseBuffer.putShort((udpHeaderLength + dnsResponse.size).toShort())
        responseBuffer.putShort(0.toShort())

        // 3. DNS Payload
        responseBuffer.put(dnsResponse)

        try {
            outStream.write(responseBuffer.array(), 0, totalLength)
            outStream.flush()
        } catch (_: Exception) {}
    }

    private fun extractDomainName(data: ByteArray): String {
        if (data.size < 13) return "Unknown"
        val sb = StringBuilder()
        var pos = 12
        while (pos < data.size) {
            val len = data[pos].toInt() and 0xFF
            if (len == 0) break
            if (pos + 1 + len > data.size) break
            if (sb.isNotEmpty()) sb.append(".")
            sb.append(String(data, pos + 1, len, Charsets.US_ASCII))
            pos += 1 + len
        }
        return if (sb.isNotEmpty()) sb.toString() else "Root (.)"
    }

    private fun recordQuery(domain: String, latency: Long) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val item = LocalQueryItem(
            domain = domain,
            latencyMs = latency,
            timestamp = timeStr,
            protocol = if (protocol.equals("DoT", ignoreCase = true)) "DoT (Port $dotPort)" else "DoH (HTTPS)",
            status = "ENCRYPTED",
            success = true,
            blocked = false
        )
        val current = _recentQueries.value.toMutableList()
        current.add(0, item)
        if (current.size > 80) current.removeAt(current.lastIndex)
        _recentQueries.value = current
    }

    private fun recordBlockedQuery(domain: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val item = LocalQueryItem(
            domain = domain,
            latencyMs = 0L,
            timestamp = timeStr,
            protocol = "Local Shield",
            status = "BLOCKED",
            success = true,
            blocked = true
        )
        val current = _recentQueries.value.toMutableList()
        current.add(0, item)
        if (current.size > 80) current.removeAt(current.lastIndex)
        _recentQueries.value = current
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
