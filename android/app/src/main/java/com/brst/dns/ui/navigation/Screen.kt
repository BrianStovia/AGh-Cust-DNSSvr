package com.brst.dns.ui.navigation

import androidx.annotation.DrawableRes
import com.brst.dns.R

sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int) {
    object Dashboard : Screen("dashboard", "Dashboard", R.drawable.ic_dashboard)
    object DohVpn : Screen("doh_vpn", "DoH Shield", R.drawable.ic_vpn)
    object QueryLog : Screen("query_log", "Query Log", R.drawable.ic_logs)
    object Devices : Screen("devices", "Perangkat", R.drawable.ic_devices)
    object Settings : Screen("settings", "Pengaturan", R.drawable.ic_settings)

    companion object {
        val bottomNavItems = listOf(Dashboard, DohVpn, QueryLog, Devices, Settings)
    }
}
