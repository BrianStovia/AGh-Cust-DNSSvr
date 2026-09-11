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
    private var packetProcessor: DohDnsPacketProcessor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Menghubungkan ke DoH..."))
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            val prefs = BrstDnsApp.instance.preferences
            val dohEndpoint = prefs.getEffectiveDohEndpoint()

            val builder = Builder()
                .setSession("BRST DoH Security Shield")
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

            // Disallow this app from VPN routing to completely prevent loopbacks
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

            packetProcessor = DohDnsPacketProcessor(this, dohEndpoint, outStream, serviceScope)

            _isRunning.value = true
            prefs.setDohVpnActive(true)

            // Listen to queries count to update notification & state
            serviceScope.launch {
                packetProcessor?.totalQueries?.collect { count ->
                    _queryCount.value = count
                    updateNotification("Aktif • $count query terenkripsi ($dohEndpoint)")
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
        BrstDnsApp.instance.preferences.setDohVpnActive(false)
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
            .setContentTitle(getString(R.string.doh_notification_title))
            .setContentText(text)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_shield, getString(R.string.doh_notification_stop), pendingStop)
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
    }
}
