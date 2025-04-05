package com.substituemanagment.managment.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.substituemanagment.managment.ui.screens.HomeScreen
import com.substituemanagment.managment.ui.screens.TeachersScreen
import com.substituemanagment.managment.ui.screens.SubstituteScreen
import com.substituemanagment.managment.ui.screens.SubstitutionsScreen
import com.substituemanagment.managment.ui.screens.SettingsScreen
import com.substituemanagment.managment.ui.screens.FileUploadScreen
import com.substituemanagment.managment.ui.screens.ProcessScreen
import com.substituemanagment.managment.ui.screens.AlgorithmTestingScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Teachers : Screen("teachers")
    object Substitutions : Screen("substitutions")
    object Settings : Screen("settings")
    object FileUpload : Screen("file_upload")
    object Process : Screen("process")
    object ViewSubstitutions : Screen("view_substitutions")
    object AlgorithmTesting : Screen("algorithm_testing")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Teachers.route) {
            TeachersScreen()
        }
        composable(Screen.Substitutions.route) {
            SubstituteScreen(navController = navController)
        }
        composable(Screen.ViewSubstitutions.route) {
            SubstitutionsScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(Screen.FileUpload.route) {
            FileUploadScreen(navController)
        }
        composable(Screen.Process.route) {
            ProcessScreen()
        }
        composable(Screen.AlgorithmTesting.route) {
            AlgorithmTestingScreen()
        }
    }
} 