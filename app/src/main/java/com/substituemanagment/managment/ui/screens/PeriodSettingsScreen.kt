package com.substituemanagment.managment.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.substituemanagment.managment.data.PeriodSetting
import com.substituemanagment.managment.data.PeriodSettings
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var periods by remember { mutableStateOf(PeriodSettings.getPeriodsSettings(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Period Settings") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (hasUnsavedChanges) {
                            // Show confirmation dialog before navigating back
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "You have unsaved changes. Save now?",
                                    actionLabel = "Save",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    isSaving = true
                                    val saved = PeriodSettings.savePeriodsSettings(context, periods)
                                    isSaving = false
                                    if (saved) {
                                        snackbarHostState.showSnackbar(
                                            message = "Settings saved successfully",
                                            duration = SnackbarDuration.Short
                                        )
                                        hasUnsavedChanges = false
                                        navController.navigateUp()
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to save settings",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    navController.navigateUp()
                                }
                            }
                        } else {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                isSaving = true
                                if (PeriodSettings.savePeriodsSettings(context, periods)) {
                                    snackbarHostState.showSnackbar(
                                        message = "Settings saved successfully",
                                        duration = SnackbarDuration.Short
                                    )
                                    hasUnsavedChanges = false
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to save settings",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                isSaving = false
                            }
                        }) {
                            Icon(
                                Icons.Default.Save, 
                                contentDescription = "Save",
                                tint = if (hasUnsavedChanges) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Period")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Configure School Periods",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Set the time ranges for each period in your school day. These periods will be used throughout the app for scheduling and viewing timetables.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Changes will be saved in the same location as your timetable files.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Period list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(periods) { index, period ->
                    PeriodItem(
                        period = period,
                        onUpdate = { updatedPeriod ->
                            val newList = periods.toMutableList()
                            newList[index] = updatedPeriod
                            periods = newList
                            hasUnsavedChanges = true
                        },
                        onDelete = {
                            val newList = periods.toMutableList()
                            newList.removeAt(index)
                            // Update period numbers to be consecutive
                            periods = newList.mapIndexed { i, p ->
                                p.copy(periodNumber = i + 1)
                            }
                            hasUnsavedChanges = true
                        }
                    )
                }
            }
            
            if (periods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Periods Configured",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to add your first period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Restore defaults button
            Button(
                onClick = {
                    periods = PeriodSettings.getPeriodsSettings(context)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Default periods restored",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Defaults")
            }
            
            // Add Period Dialog
            if (showAddDialog) {
                var newPeriodNumber by remember { mutableStateOf((periods.maxOfOrNull { it.periodNumber } ?: 0) + 1) }
                var newStartTime by remember { mutableStateOf("08:00") }
                var newEndTime by remember { mutableStateOf("08:45") }
                var newIsActive by remember { mutableStateOf(true) }
                var startTimeError by remember { mutableStateOf(false) }
                var endTimeError by remember { mutableStateOf(false) }
                
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("Add New Period") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Enter details for the new period",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            // Period Number
                            OutlinedTextField(
                                value = newPeriodNumber.toString(),
                                onValueChange = { value ->
                                    val intValue = value.toIntOrNull()
                                    if (intValue != null && intValue > 0) {
                                        newPeriodNumber = intValue
                                    }
                                },
                                label = { Text("Period Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Start Time
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newStartTime,
                                    onValueChange = { newStartTime = it },
                                    readOnly = true,
                                    label = { Text("Start Time") },
                                    isError = startTimeError,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            // Show time picker
                                            val hour = newStartTime.split(":")[0].toInt()
                                            val minute = newStartTime.split(":")[1].toInt()
                                            
                                            val timePickerDialog = TimePickerDialog(
                                                context,
                                                { _, selectedHour, selectedMinute ->
                                                    newStartTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                                    startTimeError = false
                                                },
                                                hour,
                                                minute,
                                                true
                                            )
                                            timePickerDialog.show()
                                        }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                OutlinedTextField(
                                    value = newEndTime,
                                    onValueChange = { newEndTime = it },
                                    readOnly = true,
                                    label = { Text("End Time") },
                                    isError = endTimeError,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            // Show time picker
                                            val hour = newEndTime.split(":")[0].toInt()
                                            val minute = newEndTime.split(":")[1].toInt()
                                            
                                            val timePickerDialog = TimePickerDialog(
                                                context,
                                                { _, selectedHour, selectedMinute ->
                                                    newEndTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                                    endTimeError = false
                                                },
                                                hour,
                                                minute,
                                                true
                                            )
                                            timePickerDialog.show()
                                        }
                                )
                            }
                            
                            // Active Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Period Active",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = newIsActive,
                                    onCheckedChange = { newIsActive = it }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Basic validation
                                try {
                                    val startTime = LocalTime.parse(newStartTime, timeFormat)
                                    val endTime = LocalTime.parse(newEndTime, timeFormat)
                                    
                                    if (startTime.isAfter(endTime)) {
                                        startTimeError = true
                                        endTimeError = true
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Start time must be before end time",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        return@Button
                                    }
                                    
                                    // All validation passed, add new period
                                    val newPeriod = PeriodSetting(
                                        periodNumber = newPeriodNumber,
                                        startTime = newStartTime,
                                        endTime = newEndTime,
                                        isActive = newIsActive
                                    )
                                    
                                    val newList = periods.toMutableList()
                                    newList.add(newPeriod)
                                    // Sort by period number
                                    periods = newList.sortedBy { it.periodNumber }
                                    hasUnsavedChanges = true
                                    showAddDialog = false
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Invalid time format",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Add Period")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAddDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PeriodItem(
    period: PeriodSetting,
    onUpdate: (PeriodSetting) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )
    
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    var startTime by remember { mutableStateOf(period.startTime) }
    var endTime by remember { mutableStateOf(period.endTime) }
    var isActive by remember { mutableStateOf(period.isActive) }
    var startTimeError by remember { mutableStateOf(false) }
    var endTimeError by remember { mutableStateOf(false) }
    
    // Update parent when our local state changes
    LaunchedEffect(startTime, endTime, isActive) {
        try {
            val updatedPeriod = period.copy(
                startTime = startTime,
                endTime = endTime,
                isActive = isActive
            )
            
            // Only update if something actually changed
            if (updatedPeriod != period) {
                onUpdate(updatedPeriod)
            }
        } catch (e: Exception) {
            // Handle error, don't update
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main row (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Period number circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isActive) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${period.periodNumber}",
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Period ${period.periodNumber}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = period.formatTimeRange(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = isActive,
                    onCheckedChange = { 
                        isActive = it
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationState)
                )
            }
            
            // Expanded section
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(modifier = Modifier.padding(bottom = 16.dp))
                    
                    // Time settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(48.dp)
                        )
                        
                        OutlinedButton(
                            onClick = {
                                val hour = startTime.split(":")[0].toInt()
                                val minute = startTime.split(":")[1].toInt()
                                
                                val timePickerDialog = TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        val newStartTime = String.format(
                                            "%02d:%02d", 
                                            selectedHour, 
                                            selectedMinute
                                        )
                                        startTime = newStartTime
                                        
                                        // Check if start time is after end time
                                        try {
                                            val start = LocalTime.parse(newStartTime, timeFormat)
                                            val end = LocalTime.parse(endTime, timeFormat)
                                            startTimeError = start.isAfter(end)
                                            if (startTimeError) {
                                                endTimeError = true
                                            }
                                        } catch (e: Exception) {
                                            startTimeError = true
                                        }
                                    },
                                    hour,
                                    minute,
                                    true
                                )
                                timePickerDialog.show()
                            },
                            border = BorderStroke(
                                1.dp, 
                                if (startTimeError) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = startTime,
                                color = if (startTimeError) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "End:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(48.dp)
                        )
                        
                        OutlinedButton(
                            onClick = {
                                val hour = endTime.split(":")[0].toInt()
                                val minute = endTime.split(":")[1].toInt()
                                
                                val timePickerDialog = TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        val newEndTime = String.format(
                                            "%02d:%02d", 
                                            selectedHour, 
                                            selectedMinute
                                        )
                                        endTime = newEndTime
                                        
                                        // Check if end time is before start time
                                        try {
                                            val start = LocalTime.parse(startTime, timeFormat)
                                            val end = LocalTime.parse(newEndTime, timeFormat)
                                            endTimeError = end.isBefore(start)
                                            if (endTimeError) {
                                                startTimeError = true
                                            }
                                        } catch (e: Exception) {
                                            endTimeError = true
                                        }
                                    },
                                    hour,
                                    minute,
                                    true
                                )
                                timePickerDialog.show()
                            },
                            border = BorderStroke(
                                1.dp, 
                                if (endTimeError) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = endTime,
                                color = if (endTimeError) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Delete button
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Period")
                    }
                    
                    // Error message if times are invalid
                    if (startTimeError || endTimeError) {
                        Text(
                            text = "Start time must be before end time",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
} 