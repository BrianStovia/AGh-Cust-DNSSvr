package com.brst.dns

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.brst.dns.data.preferences.AppPreferences

class BrstDnsApp : Application() {

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = AppPreferences(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DOH_SERVICE,
                getString(R.string.doh_service_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.doh_notification_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_DOH_SERVICE = "channel_brst_doh_vpn"
        lateinit var instance: BrstDnsApp
            private set
    }
}
