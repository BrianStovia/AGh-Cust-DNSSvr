package com.brst.dns.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.brst.dns.ui.screens.*
import com.brst.dns.ui.viewmodel.MainViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel = viewModel)
        }
        composable(Screen.DohVpn.route) {
            DohVpnScreen(viewModel = viewModel)
        }
        composable(Screen.QueryLog.route) {
            QueryLogScreen(viewModel = viewModel)
        }
        composable(Screen.Devices.route) {
            DevicesScreen(viewModel = viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel)
        }
    }
}
