package com.substituemanagment.managment.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.substituemanagment.managment.ui.screens.AttendanceReportsScreen
import com.substituemanagment.managment.ui.screens.FileUploadScreen
import com.substituemanagment.managment.ui.screens.HomeScreen
import com.substituemanagment.managment.ui.screens.PeriodSettingsScreen
import com.substituemanagment.managment.ui.screens.ProcessScreen
import com.substituemanagment.managment.ui.screens.ScheduleScreen
import com.substituemanagment.managment.ui.screens.SettingsScreen
import com.substituemanagment.managment.ui.screens.SmsHistoryScreen
import com.substituemanagment.managment.ui.screens.SmsProcessScreen
import com.substituemanagment.managment.ui.screens.SmsSendScreen
import com.substituemanagment.managment.ui.screens.SubstituteScreen
import com.substituemanagment.managment.ui.screens.SubstitutionsScreen
import com.substituemanagment.managment.ui.screens.TeacherDetailsScreen
import com.substituemanagment.managment.ui.screens.TeachersScreen

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
        composable(
            route = "${Screen.SmsProcess.route}/{selectedTeacher}",
            arguments = listOf(
                navArgument("selectedTeacher") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val selectedTeacher = backStackEntry.arguments?.getString("selectedTeacher")
            SmsProcessScreen(navController, selectedTeacher)
        }
        composable(Screen.AttendanceReports.route) {
            AttendanceReportsScreen(navController)
        }
        composable(Screen.TeacherDetails.route) {
            TeacherDetailsScreen(navController)
        }
        composable(Screen.SmsHistory.route) {
            SmsHistoryScreen(navController)
        }
    }
} 