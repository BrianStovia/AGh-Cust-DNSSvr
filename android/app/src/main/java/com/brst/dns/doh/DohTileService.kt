package com.brst.dns.doh

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.brst.dns.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

@RequiresApi(Build.VERSION_CODES.N)
class DohTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(DohVpnService.isRunning.value)
    }

    override fun onClick() {
        super.onClick()
        val isCurrentlyRunning = DohVpnService.isRunning.value
        if (isCurrentlyRunning) {
            val stopIntent = Intent(this, DohVpnService::class.java).apply {
                action = DohVpnService.ACTION_STOP
            }
            startService(stopIntent)
            updateTileState(false)
        } else {
            val vpnPrepare = VpnService.prepare(this)
            if (vpnPrepare != null) {
                // Needs VPN permission, open main activity
                val appIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivityAndCollapse(appIntent)
            } else {
                val startIntent = Intent(this, DohVpnService::class.java).apply {
                    action = DohVpnService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(startIntent)
                } else {
                    startService(startIntent)
                }
                updateTileState(true)
            }
        }
    }

    private fun updateTileState(active: Boolean) {
        qsTile?.let { tile ->
            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = if (active) "BRST DoT (ON)" else "BRST DoT (OFF)"
            tile.updateTile()
        }
    }
}
