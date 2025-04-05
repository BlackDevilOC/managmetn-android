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
import com.substituemanagment.managment.ui.screens.ScheduleScreen
import com.substituemanagment.managment.ui.screens.PeriodSettingsScreen
import com.substituemanagment.managment.ui.screens.SmsSendScreen

sealed class Screen(val route: String) {
    // Main screens accessible from bottom navigation
    object Home : Screen("home")
    object Teachers : Screen("teachers")
    // Renamed from "Substitutions" to "Assign" in the UI
    object Assign : Screen("assign") // Attendance Management Screen
    object SmsSend : Screen("sms_send") // SMS Notifications
    object Settings : Screen("settings")
    
    // Secondary screens
    object FileUpload : Screen("file_upload")
    object Process : Screen("process")
    object ViewSubstitutions : Screen("view_substitutions") // Assigned Substitutes List
    object Schedule : Screen("schedule")
    object PeriodSettings : Screen("period_settings")
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
        composable(Screen.Assign.route) {
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
            ProcessScreen(navController)
        }
        composable(Screen.Schedule.route) {
            ScheduleScreen(navController)
        }
        composable(Screen.PeriodSettings.route) {
            PeriodSettingsScreen(navController)
        }
        composable(Screen.SmsSend.route) {
            SmsSendScreen(navController)
        }
    }
} 