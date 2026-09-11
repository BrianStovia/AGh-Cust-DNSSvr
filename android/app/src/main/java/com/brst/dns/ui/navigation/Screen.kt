package com.brst.dns.ui.navigation

import androidx.annotation.DrawableRes
import com.brst.dns.R

sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int) {
    object Home : Screen("home", "DoT Shield", R.drawable.ic_shield)
    object Config : Screen("config", "Konfigurasi", R.drawable.ic_settings)
    object Logs : Screen("logs", "Log Query", R.drawable.ic_logs)

    companion object {
        val bottomNavItems = listOf(Home, Config, Logs)
    }
}
