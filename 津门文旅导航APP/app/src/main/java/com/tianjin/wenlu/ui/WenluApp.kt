package com.tianjin.wenlu.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tianjin.wenlu.data.SessionManager
import com.tianjin.wenlu.ui.screens.BudgetScreen
import com.tianjin.wenlu.ui.screens.FavoritesScreen
import com.tianjin.wenlu.ui.screens.HomeScreen
import com.tianjin.wenlu.ui.screens.LoginScreen
import com.tianjin.wenlu.ui.screens.MyRoutesScreen
import com.tianjin.wenlu.ui.screens.ProfileScreen
import com.tianjin.wenlu.ui.screens.RecommendScreen
import com.tianjin.wenlu.ui.screens.RoutePlanScreen
import com.tianjin.wenlu.ui.screens.ScenicDetailScreen
import com.tianjin.wenlu.ui.screens.ScenicListScreen
import com.tianjin.wenlu.ui.screens.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val SCENIC_DETAIL = "scenic/{id}"
    const val BUDGET = "budget"
    const val SETTINGS = "settings"
    const val MY_ROUTES = "my_routes"
    const val FAVORITES = "favorites"

    fun scenicDetail(id: Int) = "scenic/$id"
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab("home", "首页", Icons.Filled.Home),
    BottomTab("scenics", "景点", Icons.Filled.LocationOn),
    BottomTab("route", "路线", Icons.Filled.Map),
    BottomTab("recommend", "推荐", Icons.Filled.Star),
    BottomTab("profile", "我的", Icons.Filled.Person),
)

@Composable
fun WenluApp() {
    val navController = rememberNavController()
    val start = if (SessionManager.isLoggedIn) Routes.MAIN else Routes.LOGIN

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.MAIN) {
            MainScaffold(
                onOpenScenic = { id -> navController.navigate(Routes.scenicDetail(id)) },
                onOpenBudget = { navController.navigate(Routes.BUDGET) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenMyRoutes = { navController.navigate(Routes.MY_ROUTES) },
                onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                onLogout = {
                    SessionManager.clear()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SCENIC_DETAIL) { entry ->
            val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 1
            ScenicDetailScreen(scenicId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.BUDGET) {
            BudgetScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MY_ROUTES) {
            MyRoutesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainScaffold(
    onOpenScenic: (Int) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMyRoutes: () -> Unit,
    onOpenFavorites: () -> Unit,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    onOpenScenic = onOpenScenic,
                    onOpenBudget = onOpenBudget,
                    onOpenRoute = { navController.navigate("route") { launchSingleTop = true } },
                )
            }
            composable("scenics") {
                ScenicListScreen(onOpenScenic = onOpenScenic)
            }
            composable("route") {
                RoutePlanScreen(onOpenScenic = onOpenScenic)
            }
            composable("recommend") {
                RecommendScreen(onOpenScenic = onOpenScenic)
            }
            composable("profile") {
                ProfileScreen(
                    onOpenSettings = onOpenSettings,
                    onOpenMyRoutes = onOpenMyRoutes,
                    onOpenFavorites = onOpenFavorites,
                    onOpenBudget = onOpenBudget,
                    onLogout = onLogout,
                )
            }
        }
    }
}
