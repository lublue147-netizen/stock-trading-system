package com.stockmarket.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockmarket.app.data.local.WatchlistPreferences
import com.stockmarket.app.data.repository.StockRepository
import com.stockmarket.app.ui.screens.detail.StockDetailScreen
import com.stockmarket.app.ui.screens.detail.StockDetailViewModel
import com.stockmarket.app.ui.screens.search.SearchScreen
import com.stockmarket.app.ui.screens.search.SearchViewModel
import com.stockmarket.app.ui.screens.sector.SectorDetailScreen
import com.stockmarket.app.ui.screens.sector.SectorDetailViewModel
import com.stockmarket.app.ui.screens.settings.SettingsScreen
import com.stockmarket.app.ui.screens.settings.SettingsViewModel
import com.stockmarket.app.ui.screens.watchlist.WatchlistScreen
import com.stockmarket.app.ui.screens.watchlist.WatchlistViewModel

sealed class Screen(val route: String) {
    object Watchlist : Screen("watchlist")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object StockDetail : Screen("stock_detail/{symbol}") {
        fun createRoute(symbol: String) = "stock_detail/$symbol"
    }
    object SectorDetail : Screen("sector_detail/{bkCode}/{sectorName}") {
        fun createRoute(bkCode: String, sectorName: String): String {
            val encodedName = try {
                java.net.URLEncoder.encode(sectorName, "UTF-8")
            } catch (_: Exception) {
                sectorName
            }
            return "sector_detail/$bkCode/$encodedName"
        }
    }
}

@Composable
fun AppNavigation(
    preferences: WatchlistPreferences,
    repository: StockRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Watchlist.route
    ) {
        composable(Screen.Watchlist.route) {
            val viewModel = remember { WatchlistViewModel(repository) }
            WatchlistScreen(
                viewModel = viewModel,
                onStockClick = { symbol ->
                    navController.navigate(Screen.StockDetail.createRoute(symbol))
                },
                onSectorClick = { bkCode, name ->
                    navController.navigate(Screen.SectorDetail.createRoute(bkCode, name))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.StockDetail.route,
            arguments = listOf(navArgument("symbol") { type = NavType.StringType })
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: "600519.SS"
            val viewModel = remember(symbol) { StockDetailViewModel(symbol, repository) }
            StockDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SectorDetail.route,
            arguments = listOf(
                navArgument("bkCode") { type = NavType.StringType },
                navArgument("sectorName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bkCode = backStackEntry.arguments?.getString("bkCode") ?: "BK1638"
            val rawName = backStackEntry.arguments?.getString("sectorName") ?: "板块"
            val sectorName = try {
                java.net.URLDecoder.decode(rawName, "UTF-8")
            } catch (_: Exception) {
                rawName
            }
            val viewModel = remember(bkCode) { SectorDetailViewModel(bkCode, sectorName, repository) }
            SectorDetailScreen(
                viewModel = viewModel,
                onStockClick = { symbol ->
                    navController.navigate(Screen.StockDetail.createRoute(symbol))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            val viewModel = remember { SearchViewModel(repository) }
            SearchScreen(
                viewModel = viewModel,
                onStockClick = { symbol ->
                    navController.navigate(Screen.StockDetail.createRoute(symbol))
                },
                onSectorClick = { bkCode, name ->
                    navController.navigate(Screen.SectorDetail.createRoute(bkCode, name))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel = remember { SettingsViewModel(preferences, repository) }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
