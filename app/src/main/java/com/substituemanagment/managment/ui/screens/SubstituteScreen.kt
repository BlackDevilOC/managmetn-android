package com.substituemanagment.managment.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.substituemanagment.managment.data.AbsentTeacherData
import com.substituemanagment.managment.data.SubstituteInfo
import com.substituemanagment.managment.data.TeacherAssignmentInfo
import com.substituemanagment.managment.data.TeacherDataManager
import com.substituemanagment.managment.algorithm.SubstituteManager
import com.substituemanagment.managment.algorithm.models.SubstituteAssignment
import com.substituemanagment.managment.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstituteScreen(
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val teacherDataManager = remember { TeacherDataManager(context) }
    val substituteManager = remember { SubstituteManager(context) }
    
    var absentTeachers by remember { mutableStateOf<List<AbsentTeacherData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAssignmentDialog by remember { mutableStateOf(false) }
    var selectedTeacher by remember { mutableStateOf<AbsentTeacherData?>(null) }
    var currentDay by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show snackbar when needed
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }

    // Calculate current day
    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        currentDay = dayFormat.format(calendar.time).lowercase()
    }

    // Load absent teachers from file
    fun loadAbsentTeachers() {
        isLoading = true
        try {
            // Only load teachers who don't have a substitute assigned yet
            absentTeachers = teacherDataManager.getAbsentTeachersWithoutSubstitutes().toList()
            Log.d("SubstituteScreen", "Loaded ${absentTeachers.size} absent teachers without substitutes")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SubstituteScreen", "Error loading absent teachers: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        loadAbsentTeachers()
    }

    // Function to assign substitutes for a teacher
    fun assignSubstitutes(teacher: AbsentTeacherData, day: String) {
        coroutineScope.launch {
            isLoading = true
            try {
                // Load the data first
                withContext(Dispatchers.IO) {
                    substituteManager.loadData()
                }

                // Assign substitutes
                val assignments = substituteManager.assignSubstitutes(teacher.name, day)
                
                if (assignments.isNotEmpty()) {
                    // Create a SubstituteInfo object from the assignments
                    val firstAssignment = assignments.first()
                    
                    // Group assignments by period to avoid duplicate periods
                    val uniquePeriods = assignments
                        .distinctBy { it.period }
                        .map { 
                            TeacherAssignmentInfo(
                                period = it.period,
                                className = it.className
                            )
                        }
                    
                    val substitute = SubstituteInfo(
                        name = firstAssignment.substitute,
                        phone = firstAssignment.substitutePhone,
                        assignmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                        periods = uniquePeriods
                    )
                    
                    // Update the teacher in the absent teachers list
                    val success = teacherDataManager.updateTeacherWithSubstitute(teacher.name, substitute)
                    if (success) {
                        snackbarMessage = "Assigned substitutes for ${teacher.name}"
                        showSnackbar = true
                        
                        // Show summary of assignments
                        val assignmentSummary = assignments
                            .groupBy { it.substitute }
                            .map { (sub, assignments) -> 
                                "$sub: ${assignments.map { a -> "Period ${a.period}" }.joinToString(", ")}" 
                            }
                            .joinToString("\n")
                        
                        Log.d("SubstituteScreen", "Assignment summary:\n$assignmentSummary")
                    } else {
                        snackbarMessage = "Failed to assign substitute to ${teacher.name}"
                        showSnackbar = true
                    }
                    
                    // Reload the list
                    loadAbsentTeachers()
                } else {
                    snackbarMessage = "No suitable substitutes found for ${teacher.name}"
                    showSnackbar = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                snackbarMessage = "Error: ${e.message}"
                showSnackbar = true
            } finally {
                isLoading = false
                selectedTeacher = null
                showAssignmentDialog = false
            }
        }
    }

    // Function to batch assign substitutes for all absent teachers
    fun assignAllSubstitutes() {
        coroutineScope.launch {
            isLoading = true
            try {
                // Load the data first
                withContext(Dispatchers.IO) {
                    substituteManager.loadData()
                }

                var assignedCount = 0
                var failedCount = 0

                // Assign substitutes for all absent teachers
                for (teacher in absentTeachers) {
                    val assignments = substituteManager.assignSubstitutes(teacher.name, currentDay)
                    
                    if (assignments.isNotEmpty()) {
                        // Create a SubstituteInfo object from the assignments
                        val firstAssignment = assignments.first()
                        
                        // Group assignments by period to avoid duplicate periods
                        val uniquePeriods = assignments
                            .distinctBy { it.period }
                            .map { 
                                TeacherAssignmentInfo(
                                    period = it.period,
                                    className = it.className
                                )
                            }
                        
                        val substitute = SubstituteInfo(
                            name = firstAssignment.substitute,
                            phone = firstAssignment.substitutePhone,
                            assignmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                            periods = uniquePeriods
                        )
                        
                        // Update the teacher in the absent teachers list
                        val success = teacherDataManager.updateTeacherWithSubstitute(teacher.name, substitute)
                        if (success) {
                            assignedCount++
                            
                            // Log summary of the assignments for this teacher
                            val assignmentSummary = assignments
                                .groupBy { it.substitute }
                                .map { (sub, subAssignments) -> 
                                    "$sub: ${subAssignments.map { a -> "Period ${a.period}" }.joinToString(", ")}" 
                                }
                                .joinToString("\n")
                            
                            Log.d("SubstituteScreen", "Assignments for ${teacher.name}:\n$assignmentSummary")
                        } else {
                            failedCount++
                            Log.e("SubstituteScreen", "Failed to update ${teacher.name} with substitute")
                        }
                    } else {
                        failedCount++
                        Log.w("SubstituteScreen", "No substitutes found for ${teacher.name}")
                    }
                }
                
                // Show result message
                if (assignedCount > 0) {
                    snackbarMessage = "Successfully assigned substitutes for $assignedCount teachers"
                    if (failedCount > 0) {
                        snackbarMessage += " ($failedCount failed)"
                    }
                    showSnackbar = true
                } else if (failedCount > 0) {
                    snackbarMessage = "Failed to assign substitutes for $failedCount teachers"
                    showSnackbar = true
                }
                
                // Reload the list
                loadAbsentTeachers()
            } catch (e: Exception) {
                e.printStackTrace()
                snackbarMessage = "Error: ${e.message}"
                showSnackbar = true
            } finally {
                isLoading = false
            }
        }
    }

    // Assignment confirmation dialog
    if (showAssignmentDialog && selectedTeacher != null) {
        AlertDialog(
            onDismissRequest = { showAssignmentDialog = false },
            title = { Text("Confirm Assignment") },
            text = { 
                Text("Assign substitutes for ${selectedTeacher?.name} on ${
                    currentDay.replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                    }
                }?") 
            },
            confirmButton = {
                Button(
                    onClick = { 
                        selectedTeacher?.let { assignSubstitutes(it, currentDay) }
                    }
                ) {
                    Text("Assign")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAssignmentDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Absences") },
                actions = {
                    IconButton(onClick = { loadAbsentTeachers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp
            ) {
                Button(
                    onClick = { navController.navigate(Screen.ViewSubstitutions.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "View Assignments",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Assigned Substitutes")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Section with Date and Assign Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Absent Teachers Today",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { assignAllSubstitutes() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = absentTeachers.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Assign Substitutes",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assign All Substitutes")
                        }
                    }
                }

                // Absent Teachers List
                if (absentTeachers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No Teachers Need Substitutes",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All absent teachers have been assigned substitutes.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = absentTeachers,
                            key = { it.id } // Use unique ID as key
                        ) { teacher ->
                            AbsentTeacherCard(
                                teacher = teacher,
                                onAssignClick = {
                                    selectedTeacher = teacher
                                    showAssignmentDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsentTeacherCard(
    teacher: AbsentTeacherData,
    onAssignClick: () -> Unit
) {
    val timestamp = remember(teacher.timestamp) {
        Instant.parse(teacher.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = teacher.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onAssignClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = "Assign Substitute",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assign")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Absent",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Marked absent at: $timestamp",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AssignmentStatusChip(
    text: String,
    isAssigned: Boolean = true
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isAssigned) MaterialTheme.colorScheme.primaryContainer 
               else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (isAssigned) MaterialTheme.colorScheme.onPrimaryContainer 
                      else MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
} 