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
    
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Period Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            if (PeriodSettings.savePeriodsSettings(context, periods)) {
                                snackbarHostState.showSnackbar(
                                    message = "Settings saved successfully",
                                    duration = SnackbarDuration.Short
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = "Failed to save settings",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
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
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
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
                        },
                        onDelete = {
                            val newList = periods.toMutableList()
                            newList.removeAt(index)
                            // Update period numbers to be consecutive
                            periods = newList.mapIndexed { i, p ->
                                p.copy(periodNumber = i + 1)
                            }
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
        }
        
        if (showAddDialog) {
            val newPeriodNumber = if (periods.isEmpty()) 1 else periods.maxOf { it.periodNumber } + 1
            var startTime by remember { mutableStateOf("08:00") }
            var endTime by remember { mutableStateOf("08:45") }
            
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Period") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Period Number: $newPeriodNumber")
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Start Time:")
                            Button(
                                onClick = {
                                    val initialTime = try {
                                        LocalTime.parse(startTime, timeFormat)
                                    } catch (e: Exception) {
                                        LocalTime.of(8, 0)
                                    }
                                    
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            startTime = String.format("%02d:%02d", hourOfDay, minute)
                                        },
                                        initialTime.hour,
                                        initialTime.minute,
                                        true
                                    ).show()
                                }
                            ) {
                                Text(startTime)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("End Time:")
                            Button(
                                onClick = {
                                    val initialTime = try {
                                        LocalTime.parse(endTime, timeFormat)
                                    } catch (e: Exception) {
                                        LocalTime.of(8, 45)
                                    }
                                    
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            endTime = String.format("%02d:%02d", hourOfDay, minute)
                                        },
                                        initialTime.hour,
                                        initialTime.minute,
                                        true
                                    ).show()
                                }
                            ) {
                                Text(endTime)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newPeriod = PeriodSetting(
                                periodNumber = newPeriodNumber,
                                startTime = startTime,
                                endTime = endTime
                            )
                            periods = periods + newPeriod
                            showAddDialog = false
                        }
                    ) {
                        Text("Add")
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
    
    // Update parent when our local state changes
    LaunchedEffect(startTime, endTime, isActive) {
        val updatedPeriod = period.copy(
            startTime = startTime,
            endTime = endTime,
            isActive = isActive
        )
        if (updatedPeriod != period) {
            onUpdate(updatedPeriod)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${period.periodNumber}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    onCheckedChange = { isActive = it },
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
                        Text("Start Time")
                        Button(
                            onClick = {
                                val initialTime = try {
                                    LocalTime.parse(startTime, timeFormat)
                                } catch (e: Exception) {
                                    LocalTime.of(8, 0)
                                }
                                
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        startTime = String.format("%02d:%02d", hourOfDay, minute)
                                    },
                                    initialTime.hour,
                                    initialTime.minute,
                                    true
                                ).show()
                            }
                        ) {
                            Text(startTime)
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("End Time")
                        Button(
                            onClick = {
                                val initialTime = try {
                                    LocalTime.parse(endTime, timeFormat)
                                } catch (e: Exception) {
                                    LocalTime.of(8, 45)
                                }
                                
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        endTime = String.format("%02d:%02d", hourOfDay, minute)
                                    },
                                    initialTime.hour,
                                    initialTime.minute,
                                    true
                                ).show()
                            }
                        ) {
                            Text(endTime)
                        }
                    }
                    
                    // Delete button
                    Button(
                        onClick = { onDelete() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Period")
                    }
                }
            }
        }
    }
} 