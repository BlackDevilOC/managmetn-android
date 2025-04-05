package com.substituemanagment.managment.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.substituemanagment.managment.data.utils.ReportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class ReportType {
    ATTENDANCE,
    SUBSTITUTE
}

data class ReportFile(
    val file: File,
    val type: ReportType,
    val displayName: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReportsScreen(navController: NavController) {
    val context = LocalContext.current
    var showAttendance by remember { mutableStateOf(true) }
    var showSubstitutes by remember { mutableStateOf(true) }
    
    val currentDate = LocalDate.now()
    val currentYear = currentDate.year
    val currentMonth = currentDate.monthValue
    
    val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
    val attendanceDir = File(baseDir, "attendance_sheets")
    val jsonBackupDir = File(baseDir, "json_backups")
    
    val reports = remember {
        mutableStateListOf<ReportFile>()
    }
    
    LaunchedEffect(showAttendance, showSubstitutes) {
        reports.clear()
        
        if (attendanceDir.exists() && showAttendance) {
            // Load CSV attendance reports
            val yearDirs = attendanceDir.listFiles { file -> file.isDirectory }
            yearDirs?.forEach { yearDir ->
                val monthDirs = yearDir.listFiles { file -> file.isDirectory }
                monthDirs?.forEach { monthDir ->
                    val csvFiles = monthDir.listFiles { file -> file.extension == "csv" }
                    csvFiles?.forEach { file ->
                        val isAttendanceSheet = file.name.contains("attendance", ignoreCase = true)
                        val isSubstituteSheet = file.name.contains("substitute", ignoreCase = true)
                        
                        if ((isAttendanceSheet && showAttendance) || (isSubstituteSheet && showSubstitutes)) {
                            val type = if (isAttendanceSheet) ReportType.ATTENDANCE else ReportType.SUBSTITUTE
                            val yearMonth = "${monthDir.name}-${yearDir.name}"
                            val displayName = formatReportName(file.name)
                            
                            reports.add(
                                ReportFile(
                                    file = file,
                                    type = type,
                                    displayName = displayName,
                                    date = yearMonth
                                )
                            )
                        }
                    }
                }
            }
        }
        
        if (jsonBackupDir.exists() && showSubstitutes) {
            // Load JSON backup files
            val yearDirs = jsonBackupDir.listFiles { file -> file.isDirectory }
            yearDirs?.forEach { yearDir ->
                val monthDirs = yearDir.listFiles { file -> file.isDirectory }
                monthDirs?.forEach { monthDir ->
                    val jsonFiles = monthDir.listFiles { file -> file.extension == "json" }
                    jsonFiles?.forEach { file ->
                        val isSubstitute = file.name.contains("substitute", ignoreCase = true)
                        
                        if (isSubstitute && showSubstitutes) {
                            val yearMonth = "${monthDir.name}-${yearDir.name}"
                            val displayName = formatReportName(file.name)
                            
                            reports.add(
                                ReportFile(
                                    file = file,
                                    type = ReportType.SUBSTITUTE,
                                    displayName = displayName,
                                    date = yearMonth
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Sort by newest first
        reports.sortByDescending { it.date }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Reports") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Filter options
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Display Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Teacher Attendance",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = showAttendance,
                            onCheckedChange = { showAttendance = it }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Substitute Assignments",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = showSubstitutes,
                            onCheckedChange = { showSubstitutes = it }
                        )
                    }
                }
            }
            
            if (reports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reports found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reports) { report ->
                        ReportItem(report) { file ->
                            openFile(context, file)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItem(report: ReportFile, onClick: (File) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(report.file) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (report.file.extension == "csv") {
                Icons.Default.Description
            } else {
                Icons.Default.InsertDriveFile
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Date: ${report.date}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Format: ${report.file.extension.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatReportName(fileName: String): String {
    // Remove file extension
    var name = fileName.substringBeforeLast(".")
    
    // Replace underscores with spaces
    name = name.replace("_", " ")
    
    // Capitalize each word
    return name.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

fun openFile(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context, 
            "${context.packageName}.fileprovider", 
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(
            Intent.createChooser(intent, "Open with")
        )
    } catch (e: Exception) {
        // Show error message
        e.printStackTrace()
    }
}

fun getMimeType(file: File): String {
    return when (file.extension.lowercase()) {
        "csv" -> "text/csv"
        "json" -> "application/json"
        else -> "*/*"
    }
} 