package com.substituemanagment.managment.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.data.AbsentTeacherData
import com.substituemanagment.managment.data.TeacherData
import java.io.File
import java.io.FileReader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Data classes for attendance records
data class AttendanceRecord(
    val date: LocalDate,
    val status: AttendanceStatus
)

enum class AttendanceStatus {
    PRESENT, ABSENT, UNKNOWN
}

data class TeacherAttendanceData(
    val teacherId: Int,
    val teacherName: String,
    val attendance: Map<LocalDate, AttendanceStatus>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReportsScreen(navController: NavController) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    val gson = remember { Gson() }
    
    // Tab selection
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("View Attendance", "Reports")
    
    // For teacher selection
    var selectedTeacher by remember { mutableStateOf<TeacherData?>(null) }
    var showTeacherDropdown by remember { mutableStateOf(false) }
    val teachersList = remember { mutableStateListOf<TeacherData>() }
    
    // For date selection
    val currentDate = LocalDate.now()
    var selectedYear by remember { mutableIntStateOf(currentDate.year) }
    var selectedMonth by remember { mutableIntStateOf(currentDate.monthValue) }
    var showYearDropdown by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    
    // Available years and months (will be populated based on data)
    val availableYears = remember { mutableStateListOf<Int>() }
    val months = (1..12).map { 
        Month.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault()) 
    }
    
    // Teacher attendance data
    val teacherAttendance = remember { mutableStateListOf<TeacherAttendanceData>() }
    var filteredAttendance by remember { mutableStateOf<TeacherAttendanceData?>(null) }
    
    // Reports list for the Reports tab
    val availableReports = remember { mutableStateListOf<File>() }
    
    // Load data on composition
    LaunchedEffect(Unit) {
        isLoading = true
        
        // Get base directory
        val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
        val processedDir = File(baseDir, "processed")
        
        // Load teachers data
        val totalTeachersFile = File(processedDir, "total_teacher.json")
        if (totalTeachersFile.exists()) {
            try {
                val type = object : TypeToken<List<TeacherData>>() {}.type
                val teachers = FileReader(totalTeachersFile).use { reader ->
                    gson.fromJson<List<TeacherData>>(reader, type)
                }
                teachersList.clear()
                teachersList.addAll(teachers)
                if (teachers.isNotEmpty()) {
                    selectedTeacher = teachers.first()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Load absent teachers data
        val absentTeachersFile = File(baseDir, "absent_teachers.json")
        if (absentTeachersFile.exists()) {
            try {
                val type = object : TypeToken<Set<AbsentTeacherData>>() {}.type
                val absentTeachers = FileReader(absentTeachersFile).use { reader ->
                    gson.fromJson<Set<AbsentTeacherData>>(reader, type)
                }
                
                // Process absent teachers data into attendance records
                val attendanceMap = mutableMapOf<String, MutableMap<LocalDate, AttendanceStatus>>()
                
                // First collect all the dates to establish the range
                val allDates = mutableSetOf<LocalDate>()
                absentTeachers.forEach { absentTeacher ->
                    try {
                        val timestamp = Instant.parse(absentTeacher.timestamp)
                        val date = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()).toLocalDate()
                        allDates.add(date)
                        
                        // Initialize the map for this teacher if needed
                        if (!attendanceMap.containsKey(absentTeacher.name)) {
                            attendanceMap[absentTeacher.name] = mutableMapOf()
                        }
                        
                        // Mark this date as absent
                        attendanceMap[absentTeacher.name]!![date] = AttendanceStatus.ABSENT
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Get the first and last date in our data
                if (allDates.isNotEmpty()) {
                    val firstDate = allDates.minOf { it }
                    val lastDate = allDates.maxOf { it }
                    
                    // Collect available years for the dropdown
                    val years = (firstDate.year..lastDate.year).toList()
                    availableYears.clear()
                    availableYears.addAll(years)
                    
                    // For each teacher, set UNKNOWN for dates not marked as ABSENT
                    // (instead of automatically marking them as PRESENT)
                    teachersList.forEach { teacher ->
                        val teacherName = teacher.name
                        if (!attendanceMap.containsKey(teacherName)) {
                            attendanceMap[teacherName] = mutableMapOf()
                        }
                        
                        // We don't need to fill in anything - dates not in the map are considered UNKNOWN
                        // This is a change from the previous version where we assumed PRESENT
                    }
                    
                    // Convert to our data structure
                    val attendanceData = teachersList.mapIndexed { index, teacher ->
                        TeacherAttendanceData(
                            teacherId = index,
                            teacherName = teacher.name,
                            attendance = attendanceMap[teacher.name] ?: emptyMap()
                        )
                    }
                    
                    teacherAttendance.clear()
                    teacherAttendance.addAll(attendanceData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Load available reports
        val attendanceSheetsDir = File(baseDir, "attendance_sheets")
        if (attendanceSheetsDir.exists()) {
            val allReports = mutableListOf<File>()
            attendanceSheetsDir.walkTopDown().forEach { file ->
                if (file.isFile && (file.extension == "csv" || file.extension == "json")) {
                    allReports.add(file)
                }
            }
            availableReports.clear()
            availableReports.addAll(allReports)
        }
        
        isLoading = false
    }
    
    // Update filtered attendance data when selection changes
    LaunchedEffect(selectedTeacher, selectedYear, selectedMonth) {
        filteredAttendance = if (selectedTeacher != null) {
            teacherAttendance.find { it.teacherName == selectedTeacher?.name }?.let { teacherData ->
                // Filter attendance for selected year and month
                val filteredAttendanceMap = teacherData.attendance.filter { (date, _) ->
                    date.year == selectedYear && date.monthValue == selectedMonth
                }
                TeacherAttendanceData(
                    teacherId = teacherData.teacherId,
                    teacherName = teacherData.teacherName,
                    attendance = filteredAttendanceMap
                )
            }
        } else {
            null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Records") },
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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (selectedTab) {
                0 -> {
                    // View Attendance Tab
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading attendance data...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else if (teachersList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No teacher data available",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Teacher, Year, Month selection
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
                                    text = "Select Teacher and Date",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Teacher dropdown
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showTeacherDropdown = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null)
                                            Text(selectedTeacher?.name ?: "Select Teacher")
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showTeacherDropdown,
                                        onDismissRequest = { showTeacherDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        teachersList.forEach { teacher ->
                                            DropdownMenuItem(
                                                text = { Text(teacher.name) },
                                                onClick = {
                                                    selectedTeacher = teacher
                                                    showTeacherDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                // Row for year and month selection
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Year dropdown
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showYearDropdown = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Year: $selectedYear")
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                        
                                        DropdownMenu(
                                            expanded = showYearDropdown,
                                            onDismissRequest = { showYearDropdown = false }
                                        ) {
                                            availableYears.forEach { year ->
                                                DropdownMenuItem(
                                                    text = { Text(year.toString()) },
                                                    onClick = {
                                                        selectedYear = year
                                                        showYearDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Month dropdown
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showMonthDropdown = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Month: ${Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.getDefault())}")
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                        
                                        DropdownMenu(
                                            expanded = showMonthDropdown,
                                            onDismissRequest = { showMonthDropdown = false }
                                        ) {
                                            months.forEachIndexed { index, month ->
                                                DropdownMenuItem(
                                                    text = { Text(month) },
                                                    onClick = {
                                                        selectedMonth = index + 1
                                                        showMonthDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Display the calendar for the selected teacher
                        if (filteredAttendance != null) {
                            AttendanceCalendar(
                                teacherName = filteredAttendance!!.teacherName,
                                year = selectedYear,
                                month = selectedMonth,
                                attendanceData = filteredAttendance!!.attendance
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No attendance data available for selected teacher and date",
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Reports Tab (Show available reports to download)
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading reports...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else if (availableReports.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reports available",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "Available Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableReports) { file ->
                                ReportFileItem(file) { fileToOpen ->
                                    openFile(context, fileToOpen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceCalendar(
    teacherName: String,
    year: Int,
    month: Int,
    attendanceData: Map<LocalDate, AttendanceStatus>
) {
    val yearMonth = YearMonth.of(year, month)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Teacher name and month/year header
        Text(
            text = "$teacherName - ${Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())} $year",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Calendar grid
        var currentDay = 1
        
        // Calculate how many rows we need (maximum 6)
        val numRows = (daysInMonth + firstDayOfWeek - 1 + 6) / 7
        
        for (row in 0 until numRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                for (col in 1..7) {
                    val dayPosition = row * 7 + col
                    val dayOfMonth = dayPosition - (firstDayOfWeek - 1)
                    
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = LocalDate.of(year, month, dayOfMonth)
                        val attendanceStatus = attendanceData[date] ?: AttendanceStatus.UNKNOWN
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayOfMonth.toString(),
                                    fontSize = 14.sp
                                )
                                
                                if (attendanceStatus != AttendanceStatus.UNKNOWN) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (attendanceStatus) {
                                                    AttendanceStatus.PRESENT -> Color.Green.copy(alpha = 0.7f)
                                                    AttendanceStatus.ABSENT -> Color.Red.copy(alpha = 0.7f)
                                                    AttendanceStatus.UNKNOWN -> Color.Transparent
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (attendanceStatus) {
                                                AttendanceStatus.PRESENT -> "P"
                                                AttendanceStatus.ABSENT -> "A"
                                                AttendanceStatus.UNKNOWN -> ""
                                            },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                        
                        currentDay++
                    } else {
                        // Empty box for days outside the month
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Green.copy(alpha = 0.7f))
            )
            Text(
                text = " Present  ",
                fontSize = 12.sp
            )
            
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.7f))
            )
            Text(
                text = " Absent  ",
                fontSize = 12.sp
            )
            
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Text(
                text = " Unknown",
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ReportFileItem(file: File, onClick: (File) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(file) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (file.extension == "csv") {
                Icons.Default.Description
            } else {
                Icons.Default.FileDownload
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
                    text = formatReportName(file.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Extract date from file path if possible
                val pathParts = file.absolutePath.split(File.separator)
                val datePart = pathParts.takeLast(3).joinToString("/")
                
                Text(
                    text = "Path: ${datePart}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Format: ${file.extension.uppercase()}",
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
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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