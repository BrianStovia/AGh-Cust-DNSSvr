package com.brst.dns.doh

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.brst.dns.BrstDnsApp
import com.brst.dns.MainActivity
import com.brst.dns.R
import com.brst.dns.data.model.LocalQueryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

class DohVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var packetProcessor: DotDnsPacketProcessor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Menghubungkan ke DNS Terenkripsi..."))
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            val prefs = BrstDnsApp.instance.preferences
            val protocol = prefs.protocol.value
            val dotHost = prefs.dotHost.value
            val dotPort = prefs.dotPort.value
            val dotTlsName = prefs.dotTlsServerName.value
            val dohUrl = prefs.dohUrl.value

            val builder = Builder()
                .setSession("BRST DoT Security Shield")
                .addAddress("10.255.255.2", 30)
                .addDnsServer("10.255.255.1")
                .addRoute("10.255.255.1", 32)
                .addRoute("1.1.1.1", 32)
                .addRoute("1.0.0.1", 32)
                .addRoute("8.8.8.8", 32)
                .addRoute("8.8.4.4", 32)
                .addRoute("9.9.9.9", 32)
                .addRoute("94.140.14.14", 32)
                .setMtu(1500)
                .setBlocking(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    builder.addDisallowedApplication(packageName)
                    builder.setMetered(false)
                } catch (_: Exception) {}
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                stopVpn()
                return
            }

            val inStream = FileInputStream(vpnInterface!!.fileDescriptor)
            val outStream = FileOutputStream(vpnInterface!!.fileDescriptor)

            packetProcessor = DotDnsPacketProcessor(
                vpnService = this,
                protocol = protocol,
                dotHost = dotHost,
                dotPort = dotPort,
                dotTlsServerName = dotTlsName,
                dohUrl = dohUrl,
                outStream = outStream,
                scope = serviceScope
            )

            _isRunning.value = true
            prefs.setVpnActive(true)

            // Listen to queries count & activity
            serviceScope.launch {
                packetProcessor?.totalQueries?.collect { count ->
                    _queryCount.value = count
                    val target = if (protocol.equals("DoT", ignoreCase = true)) "$dotHost:$dotPort (DoT)" else dohUrl
                    updateNotification("Aktif • $count query terenkripsi ($target)")
                }
            }

            serviceScope.launch {
                packetProcessor?.recentQueries?.collect { list ->
                    _recentQueriesList.value = list
                }
            }

            // Packet reading loop
            serviceScope.launch {
                val packet = ByteArray(32767)
                while (isActive && vpnInterface != null) {
                    try {
                        val length = inStream.read(packet)
                        if (length > 0) {
                            packetProcessor?.processPacket(packet, length)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun stopVpn() {
        _isRunning.value = false
        BrstDnsApp.instance.preferences.setVpnActive(false)
        serviceJob.cancel()

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, DohVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, BrstDnsApp.CHANNEL_DOH_SERVICE)
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentTitle("BRST DoT Security Shield")
            .setContentText(text)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_shield, "Matikan", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.brst.dns.doh.START"
        const val ACTION_STOP = "com.brst.dns.doh.STOP"
        private const val NOTIFICATION_ID = 1001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _queryCount = MutableStateFlow(0L)
        val queryCount: StateFlow<Long> = _queryCount.asStateFlow()

        private val _recentQueriesList = MutableStateFlow<List<LocalQueryItem>>(emptyList())
        val recentQueriesList: StateFlow<List<LocalQueryItem>> = _recentQueriesList.asStateFlow()
    }
}
