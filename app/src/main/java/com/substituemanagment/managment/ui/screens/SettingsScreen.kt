package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.substituemanagment.managment.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Application Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Schedule settings section
            SectionHeader(title = "Schedule")
            
            // Schedule View Button
            SettingCard(
                icon = Icons.Default.CalendarViewDay,
                title = "View Schedule",
                description = "View the current class schedule",
                onClick = { navController.navigate(Screen.Schedule.route) }
            )
            
            // Period Settings Button
            SettingCard(
                icon = Icons.Default.Schedule,
                title = "Period Settings",
                description = "Configure school periods and time slots",
                onClick = { navController.navigate(Screen.PeriodSettings.route) }
            )
            
            // Data Management section
            SectionHeader(title = "Data Management")
            
            // File Upload Button
            SettingCard(
                icon = Icons.Default.CloudUpload,
                title = "File Upload",
                description = "Upload timetable and substitution data",
                onClick = { navController.navigate(Screen.FileUpload.route) }
            )
            
            // Process Button
            SettingCard(
                icon = Icons.Default.Refresh,
                title = "Process Data",
                description = "Process uploaded data files",
                onClick = { navController.navigate(Screen.Process.route) }
            )
            
            // Reports section
            SectionHeader(title = "Reports")
            
            // Attendance Reports Button
            SettingCard(
                icon = Icons.Default.Assessment,
                title = "Attendance Reports",
                description = "View teacher attendance and substitute assignment reports",
                onClick = { navController.navigate(Screen.AttendanceReports.route) }
            )
            
            // Teacher Management section
            SectionHeader(title = "Teacher Management")
            
            // Teachers Button
            SettingCard(
                icon = Icons.Default.People,
                title = "Manage Teachers",
                description = "View and manage teacher information",
                onClick = { navController.navigate(Screen.Teachers.route) }
            )
            
            // Substitution Management section
            SectionHeader(title = "Substitution Management")
            
            // Assign Substitutes Button
            SettingCard(
                icon = Icons.Default.AssignmentInd,
                title = "Manage Absences",
                description = "Mark teacher absences and assign substitutes",
                onClick = { navController.navigate(Screen.Assign.route) }
            )
            
            // View Substitutions Button
            SettingCard(
                icon = Icons.Default.List,
                title = "View Assigned Substitutes",
                description = "Check current substitute assignments",
                onClick = { navController.navigate(Screen.ViewSubstitutions.route) }
            )
            
            // Notifications section
            SectionHeader(title = "Notifications")
            
            // SMS Send Button
            SettingCard(
                icon = Icons.Default.Send,
                title = "SMS Notifications",
                description = "Send SMS notifications to teachers",
                onClick = { navController.navigate(Screen.SmsSend.route) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 