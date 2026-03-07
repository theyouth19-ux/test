package com.voicediary.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voicediary.app.ui.screen.HomeScreen

@Composable
fun VoiceDiaryNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToRecord = {
                    navController.navigate(NavRoutes.RECORD)
                }
            )
        }
        composable(NavRoutes.RECORD) {
            // Placeholder for record screen
        }
    }
}
