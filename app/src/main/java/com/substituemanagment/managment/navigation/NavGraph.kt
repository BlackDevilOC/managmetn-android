package com.substituemanagment.managment.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.substituemanagment.managment.ui.screens.HomeScreen
import com.substituemanagment.managment.ui.screens.TeachersScreen
import com.substituemanagment.managment.ui.screens.SubstitutionsScreen
import com.substituemanagment.managment.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Teachers : Screen("teachers")
    object Substitutions : Screen("substitutions")
    object Settings : Screen("settings")
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
            HomeScreen()
        }
        composable(Screen.Teachers.route) {
            TeachersScreen()
        }
        composable(Screen.Substitutions.route) {
            SubstitutionsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
} 