package com.nikitadevktad.fintrack.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.nikitadevktad.fintrack.R
import com.nikitadevktad.fintrack.presentation.analytics.AnalyticsScreen
import com.nikitadevktad.fintrack.presentation.history.HistoryScreen
import com.nikitadevktad.fintrack.presentation.overview.OverviewScreen

@Composable
fun FinTrackApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true,
                        onClick = {
                            navController.navigate(destination.route, navOptions {
                                // Состояние вкладки восстанавливается при возврате к ней.
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            })
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Overview.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Overview.route) { OverviewScreen() }
            composable(Destination.History.route) { HistoryScreen() }
            composable(Destination.Analytics.route) { AnalyticsScreen() }
        }
    }
}

private enum class Destination(
    val route: String,
    val labelRes: Int,
    val iconRes: Int
) {
    Overview("overview", R.string.navigation_overview, R.drawable.ic_overview),
    History("history", R.string.navigation_history, R.drawable.ic_history),
    Analytics("analytics", R.string.navigation_analytics, R.drawable.ic_analytics)
}

private val bottomDestinations = Destination.entries
