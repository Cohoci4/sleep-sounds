package com.sleepsounds.app.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sleepsounds.app.R
import com.sleepsounds.app.presentation.feature.explore.ExploreScreen
import com.sleepsounds.app.presentation.feature.generate.GenerateScreen
import com.sleepsounds.app.presentation.feature.home.HomeScreen
import com.sleepsounds.app.presentation.feature.player.PlayerBottomSheet
import com.sleepsounds.app.presentation.feature.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSoundsApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Hide bottom bar on full-screen secondary destinations.
            val showBar = currentRoute in TopLevelRoutes.map { it.route }
            if (showBar) {
                NavigationBar {
                    TopLevelRoutes.forEach { dest ->
                        NavigationBarItem(
                            selected = backStackEntry?.destination?.hierarchy
                                ?.any { it.route == dest.route } == true,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.titleRes)) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onNavigateToGenerate = { navController.navigate(NavRoutes.GENERATE) },
                )
            }
            composable(NavRoutes.EXPLORE) {
                ExploreScreen()
            }
            composable(NavRoutes.PROFILE) {
                ProfileScreen()
            }
            composable(NavRoutes.GENERATE) {
                GenerateScreen(onClose = { navController.popBackStack() })
            }
        }
    }

    PlayerBottomSheet()
}

object NavRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val PROFILE = "profile"
    const val GENERATE = "generate"
}

private data class BottomBarDestination(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
)

private val TopLevelRoutes = listOf(
    BottomBarDestination(NavRoutes.HOME, R.string.tab_home, Icons.Outlined.Home),
    BottomBarDestination(NavRoutes.EXPLORE, R.string.tab_explore, Icons.Outlined.Search),
    BottomBarDestination(NavRoutes.PROFILE, R.string.tab_profile, Icons.Outlined.Person),
)

@Suppress("unused")
private val GenerateIcon: ImageVector = Icons.Outlined.AutoAwesome
