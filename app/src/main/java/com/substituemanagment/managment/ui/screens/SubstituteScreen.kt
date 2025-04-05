package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.List
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
import com.substituemanagment.managment.data.TeacherDataManager
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.algorithm.SubstituteManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstituteScreen(
    navController: NavController = rememberNavController()
) {
    val TAG = "SubstituteScreen"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var absentTeachers by remember { mutableStateOf<List<AbsentTeacherData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var assignmentResults by remember { mutableStateOf<Map<String, List<Any>>>(emptyMap()) }
    
    // Get today's day of the week
    val today = remember {
        LocalDate.now().dayOfWeek.name.lowercase()
    }
    
    // Format today's date for display
    val formattedDate = remember {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        LocalDate.now().format(formatter)
    }

    // Function to load absent teachers from file
    val loadAbsentTeachers = {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
                    val absentTeachersFile = File(baseDir, "absent_teachers.json")
                    
                    if (absentTeachersFile.exists()) {
                        val type = object : TypeToken<Set<AbsentTeacherData>>() {}.type
                        FileReader(absentTeachersFile).use { reader ->
                            val teachers: Set<AbsentTeacherData> = Gson().fromJson(reader, type)
                            // Filter to only show unassigned teachers
                            absentTeachers = teachers.filter { !it.assignedSubstitute }.toList()
                        }
                    } else {
                        absentTeachers = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading absent teachers", e)
            }
        }
    }

    // Function to assign substitute to a specific teacher
    val assignSubstituteToTeacher = { teacherName: String ->
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    val substituteManager = SubstituteManager(context)
                    substituteManager.loadData()
                    
                    val assignments = substituteManager.assignSubstitutes(teacherName, today)
                    
                    if (assignments.isNotEmpty()) {
                        // Update the teacher's assigned status
                        val teacherDataManager = TeacherDataManager(context)
                        teacherDataManager.setTeacherAssigned(teacherName, true)
                        
                        // Refresh the list
                        loadAbsentTeachers()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning substitute", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Function to assign substitutes to all unassigned teachers
    val assignAllSubstitutes = {
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    val substituteManager = SubstituteManager(context)
                    substituteManager.loadData()
                    
                    val teacherDataManager = TeacherDataManager(context)
                    
                    // Process each unassigned teacher
                    absentTeachers.forEach { teacher ->
                        val assignments = substituteManager.assignSubstitutes(teacher.name, today)
                        if (assignments.isNotEmpty()) {
                            // Update the teacher's assigned status
                            teacherDataManager.setTeacherAssigned(teacher.name, true)
                        }
                    }
                    
                    // Refresh the list
                    loadAbsentTeachers()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning all substitutes", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Initial load of absent teachers
    LaunchedEffect(Unit) {
        loadAbsentTeachers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Absences") }
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
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Assigning substitutes...")
                }
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
                            text = formattedDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { assignAllSubstitutes() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = absentTeachers.isNotEmpty()
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
                        items(absentTeachers) { teacher ->
                            AbsentTeacherCard(
                                teacher = teacher,
                                onAssignClick = { assignSubstituteToTeacher(teacher.name) }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Assign")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Marked absent at: $timestamp",
                style = MaterialTheme.typography.bodyMedium
            )
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