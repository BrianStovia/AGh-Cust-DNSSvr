package com.brst.dns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.brst.dns.ui.navigation.NavGraph
import com.brst.dns.ui.navigation.Screen
import com.brst.dns.ui.theme.BrstAccent
import com.brst.dns.ui.theme.BrstBackground
import com.brst.dns.ui.theme.BrstCardBorder
import com.brst.dns.ui.theme.BrstDnsTheme
import com.brst.dns.ui.theme.BrstPrimary
import com.brst.dns.ui.theme.BrstSurface
import com.brst.dns.ui.theme.BrstTextMuted
import com.brst.dns.ui.theme.BrstTextPrimary
import com.brst.dns.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BrstDnsTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BrstBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = BrstSurface,
                            tonalElevation = 8.dp
                        ) {
                            Screen.bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = screen.iconRes),
                                            contentDescription = screen.title,
                                            tint = if (selected) BrstAccent else BrstTextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            color = if (selected) BrstAccent else BrstTextMuted
                                        )
                                    },
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = BrstPrimary.copy(alpha = 0.2f)
                                    ),
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
