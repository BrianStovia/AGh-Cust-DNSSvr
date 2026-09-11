package com.brst.dns.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.brst.dns.ui.screens.DotConfigScreen
import com.brst.dns.ui.screens.DotHomeScreen
import com.brst.dns.ui.screens.DotLogsScreen
import com.brst.dns.ui.viewmodel.DotViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: DotViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            DotHomeScreen(viewModel = viewModel)
        }
        composable(Screen.Config.route) {
            DotConfigScreen(viewModel = viewModel)
        }
        composable(Screen.Logs.route) {
            DotLogsScreen(viewModel = viewModel)
        }
    }
}
