package com.seclass.stunner.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.seclass.stunner.ui.screen.DetectionScreen
import com.seclass.stunner.ui.screen.MainMenuScreen
import com.seclass.stunner.ui.screen.StatsScreen
import com.seclass.stunner.viewmodel.DetectionViewModel
import com.seclass.stunner.viewmodel.StatsViewModel

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object Detection : Screen("detection")
    object Stats : Screen("stats")
}

@Composable
fun AppNavigation(
    detectionViewModel: DetectionViewModel,
    statsViewModel: StatsViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.MainMenu.route) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onStartGame = { navController.navigate(Screen.Detection.route) },
                onViewStats = { navController.navigate(Screen.Stats.route) }
            )
        }
        composable(Screen.Detection.route) {
            DetectionScreen(
                viewModel = detectionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(
                viewModel = statsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
