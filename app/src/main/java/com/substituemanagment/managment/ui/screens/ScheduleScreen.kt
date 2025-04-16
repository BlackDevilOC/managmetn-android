package com.substituemanagment.managment.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.data.PeriodSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State variables
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Get current day from device
    val today = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase()
    var selectedDay by remember { mutableStateOf(today) }
    
    // View mode (list or grid)
    var viewMode by remember { mutableStateOf("list") } // "list" or "grid"
    
    var selectedTeacher by remember { mutableStateOf<String?>(null) }
    var scheduleData by remember { mutableStateOf<Map<String, List<ScheduleEntry>>>(emptyMap()) }
    var teachersList by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Get periods from PeriodSettings
    val periods by remember { mutableStateOf(PeriodSettings.getPeriodsSettings(context)) }
    var selectedPeriod by remember { mutableStateOf<Int?>(PeriodSettings.getCurrentPeriod(context)) }
    
    // Load the schedule data
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            errorMessage = null
            
            // Define the path to the processed data directory
            val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
            val processedDir = File(baseDir, "processed")
            val scheduleFile = File(processedDir, "teacher_schedules.json")
            
            if (!scheduleFile.exists()) {
                throw IOException("Schedule file not found. Please process timetable data first.")
            }
            
            // Read and parse the JSON file
            val jsonContent = scheduleFile.readText()
            val type = object : TypeToken<Map<String, List<Map<String, Any>>>>() {}.type
            val rawData = Gson().fromJson<Map<String, List<Map<String, Any>>>>(jsonContent, type)
            
            // Convert the raw data to our schedule format
            val convertedData = rawData.mapValues { (_, entries) ->
                entries.map { entry ->
                    ScheduleEntry(
                        day = entry["day"] as String,
                        period = (entry["period"] as Number).toInt(),
                        className = entry["className"] as String,
                        originalTeacher = entry["originalTeacher"] as String,
                        substitute = (entry["substitute"] as? String) ?: ""
                    )
                }
            }
            
            scheduleData = convertedData
            teachersList = convertedData.keys.toList().sorted()
            
            if (teachersList.isNotEmpty()) {
                selectedTeacher = teachersList.first()
            }
            
            isLoading = false
        } catch (e: Exception) {
            Log.e("ScheduleScreen", "Error loading schedule data", e)
            errorMessage = e.message
            isLoading = false
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Failed to load schedule data: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Toggle view mode button
                    IconButton(onClick = { viewMode = if (viewMode == "list") "grid" else "list" }) {
                        Icon(
                            if (viewMode == "list") Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Change View"
                        )
                    }
                    // Settings button
                    IconButton(onClick = { navController.navigate(Screen.PeriodSettings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Period Settings")
                    }
                    // Refresh button
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Schedule refreshed",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Unknown error occurred",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Process.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process Timetable")
                    }
                }
            } else if (scheduleData.isEmpty()) {
                EmptyScheduleView(onProcessClick = {
                    navController.navigate(Screen.Process.route)
                })
            } else {
                ScheduleContent(
                    scheduleData = scheduleData,
                    teachersList = teachersList,
                    selectedTeacher = selectedTeacher,
                    selectedDay = selectedDay,
                    viewMode = viewMode,
                    periods = periods,
                    selectedPeriod = selectedPeriod,
                    onTeacherSelected = { selectedTeacher = it },
                    onDaySelected = { selectedDay = it },
                    onPeriodSelected = { selectedPeriod = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleContent(
    scheduleData: Map<String, List<ScheduleEntry>>,
    teachersList: List<String>,
    selectedTeacher: String?,
    selectedDay: String,
    viewMode: String,
    periods: List<com.substituemanagment.managment.data.PeriodSetting>,
    selectedPeriod: Int?,
    onTeacherSelected: (String) -> Unit,
    onDaySelected: (String) -> Unit,
    onPeriodSelected: (Int?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Teacher Selection
        Text(
            text = "Select Teacher",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Teacher dropdown
        var dropdownExpanded by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTeacher ?: "",
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                teachersList.forEach { teacher ->
                    DropdownMenuItem(
                        text = { Text(teacher) },
                        onClick = { 
                            onTeacherSelected(teacher)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Days of week tabs - exclude Sunday unless there are entries for it
        val activeDays = getActiveDaysOfWeek(scheduleData, selectedTeacher)
        
        ScrollableTabRow(
            selectedTabIndex = getDayIndex(selectedDay, activeDays),
            edgePadding = 0.dp
        ) {
            activeDays.forEachIndexed { index, day ->
                Tab(
                    selected = selectedDay.equals(day, ignoreCase = true),
                    onClick = { onDaySelected(day.lowercase()) },
                    text = { Text(day.capitalize()) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Schedule for selected teacher and day
        selectedTeacher?.let { teacher ->
            val teacherSchedule = scheduleData[teacher] ?: emptyList()
            val daySchedule = teacherSchedule
                .filter { it.day.equals(selectedDay, ignoreCase = true) }
                .sortedBy { it.period }
            
            if (daySchedule.isEmpty()) {
                EmptyDaySchedule()
            } else {
                if (viewMode == "list") {
                    // List view
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(daySchedule) { entry ->
                            ScheduleEntryCard(
                                entry = entry,
                                isCurrentPeriod = entry.period == selectedPeriod
                            )
                        }
                    }
                } else {
                    // Grid view
                    GridScheduleView(
                        scheduleEntries = daySchedule,
                        periods = periods,
                        currentPeriod = selectedPeriod,
                        onPeriodSelected = onPeriodSelected
                    )
                }
            }
        }
    }
}

@Composable
fun GridScheduleView(
    scheduleEntries: List<ScheduleEntry>,
    periods: List<com.substituemanagment.managment.data.PeriodSetting>,
    currentPeriod: Int?,
    onPeriodSelected: (Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Header - shows the time periods
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty cell for the period number column
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .padding(4.dp)
            ) {
                Text(
                    text = "Period",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Column for class information
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(4.dp)
            ) {
                Text(
                    text = "Class",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Column for teacher
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(4.dp)
            ) {
                Text(
                    text = "Teacher",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        
        Divider()
        
        // Grid rows for each period
        for (periodSetting in periods.sortedBy { it.periodNumber }) {
            val entry = scheduleEntries.find { it.period == periodSetting.periodNumber }
            val isCurrentPeriod = periodSetting.periodNumber == currentPeriod
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPeriodSelected(periodSetting.periodNumber) }
                    .background(
                        if (isCurrentPeriod) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.background
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Period number column
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(64.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${periodSetting.periodNumber}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = periodSetting.formatTimeRange(),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
                
                // Class name column
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry?.className ?: "No Class",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (entry != null) FontWeight.Normal else FontWeight.Light,
                        color = if (entry != null) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                // Teacher column
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (entry != null) {
                            Text(
                                text = entry.originalTeacher,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            if (entry.substitute.isNotEmpty()) {
                                Text(
                                    text = "Sub: ${entry.substitute}",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            Text(
                                text = "—",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            
            Divider()
        }
    }
}

@Composable
fun ScheduleEntryCard(
    entry: ScheduleEntry,
    isCurrentPeriod: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPeriod) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isCurrentPeriod)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P${entry.period}",
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentPeriod)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.className,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentPeriod)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isCurrentPeriod)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (entry.substitute.isNotEmpty()) {
                            "Original: ${entry.originalTeacher}"
                        } else {
                            entry.originalTeacher
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentPeriod)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Show substitute info if available
                if (entry.substitute.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Substitute: ${entry.substitute}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Details",
                tint = if (isCurrentPeriod)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptyScheduleView(onProcessClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "No Schedule",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Schedule Data Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Process your timetable data to view schedules",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onProcessClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Process Timetable")
        }
    }
}

@Composable
fun EmptyDaySchedule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = "No Classes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No Classes Scheduled",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ScheduleEntry(
    val day: String,
    val period: Int,
    val className: String,
    val originalTeacher: String,
    val substitute: String = ""
)

// Helper function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

// Helper function to get all active days that have schedule entries
private fun getActiveDaysOfWeek(scheduleData: Map<String, List<ScheduleEntry>>, selectedTeacher: String?): List<String> {
    if (selectedTeacher == null) return listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
    
    val teacherEntries = scheduleData[selectedTeacher] ?: return listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
    val activeDays = teacherEntries.map { it.day.lowercase() }.distinct().sorted()
    
    // If there are no entries for any day, return the default weekdays
    return if (activeDays.isEmpty()) {
        listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
    } else {
        // Only include Sunday if there are entries for it
        val standardDays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
        val result = standardDays.toMutableList()
        if (activeDays.contains("sunday")) {
            result.add("sunday")
        }
        result
    }
}

private fun getDayIndex(day: String, days: List<String>): Int {
    val normalizedDay = day.lowercase()
    return days.indexOf(normalizedDay).takeIf { it >= 0 } ?: 0
} 