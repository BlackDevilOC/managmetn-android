package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.substituemanagment.managment.data.AbsentTeacherData
import com.substituemanagment.managment.data.AssignmentRecord
import com.substituemanagment.managment.data.TeacherDataManager
import com.substituemanagment.managment.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController = rememberNavController()) {
    val context = LocalContext.current
    val teacherDataManager = remember { TeacherDataManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var absentTeachers by remember { mutableStateOf<Set<AbsentTeacherData>>(emptySet()) }
    var assignmentRecords by remember { mutableStateOf<List<AssignmentRecord>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Load data
    fun loadData() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                absentTeachers = teacherDataManager.getAbsentTeachers()
                assignmentRecords = teacherDataManager.getAssignmentRecords()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRefreshing = false
            }
        }
    }
    
    // Initial load
    LaunchedEffect(Unit) {
        loadData()
    }
    
    // Filter today's assignments
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todaysAssignments = assignmentRecords.filter { it.assignmentDate == todayDateStr }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Substitution") },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Today's Overview",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${absentTeachers.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Absent Teachers",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${todaysAssignments.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Assignments Today",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Quick Actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.Teachers.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Manage Teachers")
                }
                Button(
                    onClick = { navController.navigate(Screen.Substitutions.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Manage Absences")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.ViewSubstitutions.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Assignments")
                }
                Button(
                    onClick = { navController.navigate(Screen.AlgorithmTesting.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Algorithm Testing")
                }
            }

            // Recent Substitutions
            Text(
                text = "Recent Assignments",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (assignmentRecords.isEmpty()) {
                        Text(
                            text = "No recent assignments",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Take the most recent 3 assignments
                        assignmentRecords.sortedByDescending { it.timestamp }.take(3).forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = record.absentTeacher,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Text(
                                    text = "→ ${record.substituteTeacher}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Add a divider if it's not the last item
                            if (record != assignmentRecords.sortedByDescending { it.timestamp }.take(3).last()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        
                        if (assignmentRecords.size > 3) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { navController.navigate(Screen.ViewSubstitutions.route) }
                            ) {
                                Text("View All")
                            }
                        }
                    }
                }
            }
        }
    }
} 