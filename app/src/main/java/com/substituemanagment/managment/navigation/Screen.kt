package com.substituemanagment.managment.navigation

sealed class Screen(val route: String) {
    // Main screens accessible from bottom navigation
    object Home : Screen("home")
    object Teachers : Screen("teachers")
    object Assign : Screen("assign") // Attendance Management Screen
    object SmsSend : Screen("sms_send") // SMS Notifications
    object Settings : Screen("settings")
    
    // Secondary screens
    object FileUpload : Screen("file_upload")
    object Process : Screen("process")
    object ViewSubstitutions : Screen("view_substitutions") // Assigned Substitutes List
    object Schedule : Screen("schedule")
    object PeriodSettings : Screen("period_settings")
    object AttendanceReports : Screen("attendance_reports")
    object SmsProcess : Screen("sms_process") // Phone number verification for SMS
    object TeacherDetails : Screen("teacher_details") // Teacher details screen
    object SmsHistory : Screen("sms_history") // SMS History screen
} 