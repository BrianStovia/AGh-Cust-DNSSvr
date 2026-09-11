package com.brst.dns.doh

import android.net.VpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class DohDnsPacketProcessor(
    private val vpnService: VpnService,
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

        // Custom protected socket factory to prevent VPN routing loop
        val protectedSocketFactory = object : SocketFactory() {
            private val defaultFactory = getDefault()

            override fun createSocket(): Socket {
                val s = defaultFactory.createSocket()
                vpnService.protect(s)
                return s
            }

            override fun createSocket(host: String?, port: Int): Socket {
                val s = defaultFactory.createSocket(host, port)
                vpnService.protect(s)
                return s
            }

            override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
                val s = defaultFactory.createSocket(host, port, localHost, localPort)
                vpnService.protect(s)
                return s
            }

            override fun createSocket(host: InetAddress?, port: Int): Socket {
                val s = defaultFactory.createSocket(host, port)
                vpnService.protect(s)
                return s
            }

            override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
                val s = defaultFactory.createSocket(address, port, localAddress, localPort)
                vpnService.protect(s)
                return s
            }
        }

        // Bootstrap DNS resolver to prevent deadlock when resolving DoH hostname
        val bootstrapDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                // If hostname is already an IP address
                try {
                    val ip = InetAddress.getByName(hostname)
                    return listOf(ip)
                } catch (_: Exception) {}

                // Known bootstrap IPs for popular resolvers
                when (hostname.lowercase()) {
                    "cloudflare-dns.com", "one.one.one.one" -> return listOf(
                        InetAddress.getByName("1.1.1.1"),
                        InetAddress.getByName("1.0.0.1")
                    )
                    "dns.quad9.net" -> return listOf(
                        InetAddress.getByName("9.9.9.9"),
                        InetAddress.getByName("149.112.112.112")
                    )
                    "dns.google" -> return listOf(
                        InetAddress.getByName("8.8.8.8"),
                        InetAddress.getByName("8.8.4.4")
                    )
                    "dns.adguard-dns.com" -> return listOf(
                        InetAddress.getByName("94.140.14.14"),
                        InetAddress.getByName("94.140.15.15")
                    )
                }

                // Fallback: Query Cloudflare directly via protected UDP socket
                return resolveViaProtectedSocket(hostname)
            }
        }

        OkHttpClient.Builder()
            .socketFactory(protectedSocketFactory)
            .dns(bootstrapDns)
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
            // If DoH fails (e.g. timeout or server offline), try fallback resolution to avoid breaking device connectivity
            fallbackDnsResolution(dnsQuery, dstIp, srcIp, dstPort, srcPort)
        }
    }

    private fun fallbackDnsResolution(
        dnsQuery: ByteArray,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int
    ) {
        try {
            val socket = DatagramSocket()
            vpnService.protect(socket)
            socket.soTimeout = 2000

            val packet = DatagramPacket(dnsQuery, dnsQuery.size, InetAddress.getByName("1.1.1.1"), 53)
            socket.send(packet)

            val receiveData = ByteArray(4096)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)
            socket.close()

            val dnsResponseBytes = ByteArray(receivePacket.length).apply {
                System.arraycopy(receiveData, 0, this, 0, receivePacket.length)
            }
            sendDnsResponse(dnsResponseBytes, srcIp, dstIp, srcPort, dstPort)
        } catch (_: Exception) {}
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
        responseBuffer.putShort(0.toShort()) // UDP Checksum (0 is valid for IPv4)

        // 3. DNS Payload
        responseBuffer.put(dnsResponse)

        try {
            outStream.write(responseBuffer.array(), 0, totalLength)
            outStream.flush()
        } catch (_: Exception) {}
    }

    private fun resolveViaProtectedSocket(hostname: String): List<InetAddress> {
        return try {
            val socket = DatagramSocket()
            vpnService.protect(socket)
            socket.soTimeout = 2000

            // Construct minimal standard DNS A-query packet
            val queryStream = ByteArrayOutputStream()
            val txId = (System.currentTimeMillis() and 0xFFFF).toInt()
            queryStream.write(byteArrayOf((txId shr 8).toByte(), txId.toByte())) // ID
            queryStream.write(byteArrayOf(0x01, 0x00)) // Flags: Standard query, recursion desired
            queryStream.write(byteArrayOf(0x00, 0x01)) // QDCOUNT = 1
            queryStream.write(byteArrayOf(0x00, 0x00)) // ANCOUNT = 0
            queryStream.write(byteArrayOf(0x00, 0x00)) // NSCOUNT = 0
            queryStream.write(byteArrayOf(0x00, 0x00)) // ARCOUNT = 0

            // QNAME
            for (label in hostname.split(".")) {
                queryStream.write(label.length)
                queryStream.write(label.toByteArray(Charsets.US_ASCII))
            }
            queryStream.write(0) // Root null terminator
            queryStream.write(byteArrayOf(0x00, 0x01)) // QTYPE = A (1)
            queryStream.write(byteArrayOf(0x00, 0x01)) // QCLASS = IN (1)

            val queryBytes = queryStream.toByteArray()
            val sendPacket = DatagramPacket(queryBytes, queryBytes.size, InetAddress.getByName("1.1.1.1"), 53)
            socket.send(sendPacket)

            val buffer = ByteArray(512)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)
            socket.close()

            // Parse response IP
            parseDnsAnswerIp(buffer, receivePacket.length)
        } catch (_: Exception) {
            Dns.SYSTEM.lookup(hostname)
        }
    }

    private fun parseDnsAnswerIp(data: ByteArray, length: Int): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        if (length < 12) return addresses

        val buf = ByteBuffer.wrap(data, 0, length)
        val anCount = buf.getShort(6).toInt() and 0xFFFF
        if (anCount == 0) return addresses

        // Skip header (12 bytes) and question section
        var pos = 12
        while (pos < length && data[pos].toInt() != 0) {
            val len = data[pos].toInt() and 0xFF
            if ((len and 0xC0) == 0xC0) {
                pos += 2
                break
            } else {
                pos += len + 1
            }
        }
        if (pos < length && data[pos].toInt() == 0) pos++ // Skip null byte
        pos += 4 // Skip QTYPE and QCLASS

        // Parse Answer records
        for (i in 0 until anCount) {
            if (pos >= length) break
            // Skip Name (pointer or label)
            if ((data[pos].toInt() and 0xC0) == 0xC0) {
                pos += 2
            } else {
                while (pos < length && data[pos].toInt() != 0) pos++
                if (pos < length) pos++
            }
            if (pos + 10 > length) break

            val type = (data[pos].toInt() and 0xFF shl 8) or (data[pos + 1].toInt() and 0xFF)
            val rdLength = (data[pos + 8].toInt() and 0xFF shl 8) or (data[pos + 9].toInt() and 0xFF)
            pos += 10

            if (type == 1 && rdLength == 4 && pos + 4 <= length) { // TYPE A (IPv4)
                val ipBytes = ByteArray(4)
                System.arraycopy(data, pos, ipBytes, 0, 4)
                addresses.add(InetAddress.getByAddress(ipBytes))
            }
            pos += rdLength
        }

        return if (addresses.isNotEmpty()) addresses else listOf(InetAddress.getByName("1.1.1.1"))
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
