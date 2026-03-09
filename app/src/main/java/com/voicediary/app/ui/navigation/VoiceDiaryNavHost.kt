package com.voicediary.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicediary.app.ui.screen.BackupScreen
import com.voicediary.app.ui.screen.DetailScreen
import com.voicediary.app.ui.screen.HomeScreen
import com.voicediary.app.ui.screen.ListScreen
import com.voicediary.app.ui.screen.RecordScreen
import com.voicediary.app.ui.screen.RecordViewModel
import com.voicediary.app.ui.screen.ResultScreen
import com.voicediary.app.ui.screen.SettingsScreen
import com.voicediary.app.ui.screen.SplashScreen

@Composable
fun VoiceDiaryNavHost(initialRecordType: String? = null) {
    val navController = rememberNavController()

    // 위젯에서 바로 녹음 화면으로 이동
    LaunchedEffect(initialRecordType) {
        if (initialRecordType != null) {
            navController.navigate(NavRoutes.record(initialRecordType)) {
                popUpTo(NavRoutes.SPLASH) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToRecord = { type ->
                    navController.navigate(NavRoutes.record(type))
                },
                onNavigateToList = {
                    navController.navigate(NavRoutes.LIST)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
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
                },
                onNavigateToRecord = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
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

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBackup = {
                    navController.navigate(NavRoutes.BACKUP)
                }
            )
        }

        composable(NavRoutes.BACKUP) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
