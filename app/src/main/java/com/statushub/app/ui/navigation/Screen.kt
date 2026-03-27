package com.statushub.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.statushub.app.data.model.ThemeMode
import com.statushub.app.ui.screens.home.HomeScreen
import com.statushub.app.ui.screens.onboarding.OnboardingScreen
import com.statushub.app.ui.screens.preview.PreviewScreen
import com.statushub.app.ui.screens.saved.SavedScreen
import com.statushub.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Preview : Screen("preview/{statusId}?isSaved={isSaved}&index={index}") {
        fun createRoute(statusId: String, isSaved: Boolean = false, index: Int = 0): String {
            return "preview/$statusId?isSaved=$isSaved&index=$index"
        }
    }
    object Saved : Screen("saved")
    object Settings : Screen("settings")
}

@Composable
fun StatusHubNavigation(
    navController: NavHostController,
    onThemeChanged: (ThemeMode) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.popBackStack()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onStatusClick = { status, index ->
                    navController.navigate(Screen.Preview.createRoute(status.id, false, index))
                },
                onOpenWhatsApp = {
                    // Handled in HomeScreen
                },
                onNavigateToSaved = {
                    navController.navigate(Screen.Saved.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Preview.route,
            arguments = listOf(
                navArgument("statusId") { type = NavType.StringType },
                navArgument("isSaved") { type = NavType.BoolType; defaultValue = false },
                navArgument("index") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val statusId = backStackEntry.arguments?.getString("statusId") ?: ""
            val isSaved = backStackEntry.arguments?.getBoolean("isSaved") ?: false
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            
            PreviewScreen(
                statusId = statusId,
                isSavedStatus = isSaved,
                initialIndex = index,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Saved.route) {
            SavedScreen(
                onItemClick = { savedStatus, index ->
                    navController.navigate(Screen.Preview.createRoute(savedStatus.id, true, index))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onThemeChanged = onThemeChanged,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
