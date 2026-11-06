package com.mystorage.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mystorage.ui.additem.AddItemScreen
import com.mystorage.ui.home.HomeScreen
import com.mystorage.ui.product.ProductScreen
import com.mystorage.ui.scanner.ScannerScreen
import com.mystorage.ui.settings.SettingsScreen
import com.mystorage.ui.stock.StockScreen
import kotlin.reflect.KClass

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, HomeRoute::class, "Início", Icons.Filled.Home),
    TopLevelDestination(StockRoute, StockRoute::class, "Estoque", Icons.AutoMirrored.Filled.List),
    TopLevelDestination(SettingsRoute, SettingsRoute::class, "Ajustes", Icons.Filled.Settings),
)

@Composable
fun MyStorageNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { currentDestination?.hasRoute(it.routeClass) == true }

    Scaffold(
        // Cada tela cuida dos próprios insets (status bar / TopAppBar); aqui só a barra inferior.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentDestination?.hasRoute(dest.routeClass) == true,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onScanClick = { navController.navigate(ScannerRoute) },
                    onProductClick = { navController.navigate(ProductRoute(it)) },
                )
            }
            composable<StockRoute> {
                StockScreen(
                    onProductClick = { navController.navigate(ProductRoute(it)) },
                    onScanClick = { navController.navigate(ScannerRoute) },
                )
            }
            composable<SettingsRoute> { SettingsScreen() }
            composable<ScannerRoute> {
                ScannerScreen(
                    onBarcode = { navController.navigate(AddItemRoute(it)) },
                    onWithoutBarcode = { navController.navigate(AddItemRoute(null)) },
                    onBack = navController::popBackStack,
                )
            }
            composable<AddItemRoute> {
                // Modo contínuo: depois de salvar, volta para o scanner.
                AddItemScreen(onSaved = navController::popBackStack, onBack = navController::popBackStack)
            }
            composable<ProductRoute> { entry ->
                ProductScreen(
                    productId = entry.toRoute<ProductRoute>().productId,
                    onBack = navController::popBackStack,
                )
            }
        }
    }
}
