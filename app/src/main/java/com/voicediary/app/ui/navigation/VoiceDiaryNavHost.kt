package com.voicediary.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicediary.app.ui.screen.DetailScreen
import com.voicediary.app.ui.screen.HomeScreen
import com.voicediary.app.ui.screen.ListScreen
import com.voicediary.app.ui.screen.RecordScreen
import com.voicediary.app.ui.screen.RecordViewModel
import com.voicediary.app.ui.screen.ResultScreen

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
            // Record 화면의 ViewModel을 생성
            val recordViewModel: RecordViewModel = hiltViewModel(backStackEntry)

            RecordScreen(
                type = type,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResult = {
                    navController.navigate(NavRoutes.result(type))
                },
                viewModel = recordViewModel
            )
        }

        composable(
            route = NavRoutes.RESULT,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            // Record 화면의 backStackEntry에서 같은 ViewModel을 공유
            val recordBackStackEntry = remember(backStackEntry) {
                val type = backStackEntry.arguments?.getString("type") ?: "diary"
                navController.getBackStackEntry(NavRoutes.record(type))
            }
            val recordViewModel: RecordViewModel = hiltViewModel(recordBackStackEntry)
            val type = backStackEntry.arguments?.getString("type") ?: "diary"

            ResultScreen(
                onNavigateToList = {
                    // 저장 후 Record + Result를 모두 pop하고 list로 이동
                    navController.navigate(NavRoutes.LIST) {
                        popUpTo(NavRoutes.HOME)
                    }
                },
                onNavigateToReRecord = {
                    // Result를 pop하면 Record로 돌아감 (새 녹음 가능)
                    navController.popBackStack(NavRoutes.record(type), inclusive = true)
                    navController.navigate(NavRoutes.record(type))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = recordViewModel
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
