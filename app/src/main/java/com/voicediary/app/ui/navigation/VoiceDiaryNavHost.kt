package com.voicediary.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicediary.app.ui.screen.DetailScreen
import com.voicediary.app.ui.screen.HomeScreen
import com.voicediary.app.ui.screen.ListScreen
import com.voicediary.app.ui.screen.RecordScreen

@Composable
fun VoiceDiaryNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToRecord = { type ->
                    navController.navigate(NavRoutes.record(type))
                },
                onNavigateToList = {
                    navController.navigate(NavRoutes.LIST)
                }
            )
        }

        composable(
            route = NavRoutes.RECORD,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "diary"
            RecordScreen(
                type = type,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.LIST) {
            ListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id ->
                    navController.navigate(NavRoutes.detail(id))
                }
            )
        }

        composable(
            route = NavRoutes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            DetailScreen(
                entryId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
