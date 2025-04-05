package com.substituemanagment.managment.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.substituemanagment.managment.algorithm.SubstituteManager
import com.substituemanagment.managment.algorithm.models.SubstituteAssignment
import com.substituemanagment.managment.algorithm.models.Teacher
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmTestingScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val substituteManager = remember { SubstituteManager(context) }
    
    var isLoading by remember { mutableStateOf(false) }
    var dataLoaded by remember { mutableStateOf(false) }
    var selectedTeacher by remember { mutableStateOf<Teacher?>(null) }
    var assignments by remember { mutableStateOf<List<SubstituteAssignment>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Bulk testing state
    var bulkTestingMode by remember { mutableStateOf(false) }
    var numberOfTeachers by remember { mutableIntStateOf(5) }
    var selectedDay by remember { mutableStateOf("saturday") }
    var bulkTestResults by remember { mutableStateOf<Map<String, List<SubstituteAssignment>>>(emptyMap()) }
    var bulkTestCompleted by remember { mutableStateOf(false) }
    var bulkTestResultMessage by remember { mutableStateOf("") }
    
    // Available days
    val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    
    // Load data when the screen is first displayed
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            substituteManager.loadData()
            dataLoaded = true
        } catch (e: Exception) {
            Log.e("AlgorithmTestingScreen", "Error loading data: ${e.message}", e)
            errorMessage = "Error loading data: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Algorithm Testing") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading data...")
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    substituteManager.loadData()
                                    dataLoaded = true
                                } catch (e: Exception) {
                                    errorMessage = "Error loading data: ${e.message}"
                                    Log.e("AlgorithmTestingScreen", "Error loading data: ${e.message}", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            } else if (assignments.isNotEmpty() && !bulkTestingMode) {
                // Display the results for single teacher test
                AssignmentResults(
                    assignments = assignments,
                    selectedTeacher = selectedTeacher,
                    onReset = {
                        assignments = emptyList()
                        selectedTeacher = null
                    }
                )
            } else if (bulkTestCompleted) {
                // Display bulk test results
                BulkTestResults(
                    results = bulkTestResults,
                    resultMessage = bulkTestResultMessage,
                    onReset = {
                        bulkTestCompleted = false
                        bulkTestResults = emptyMap()
                        bulkTestResultMessage = ""
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Algorithm Test",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Test Mode Selection
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Test Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !bulkTestingMode,
                                    onClick = { bulkTestingMode = false }
                                )
                                Text("Single Teacher")
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = bulkTestingMode,
                                    onClick = { bulkTestingMode = true }
                                )
                                Text("Bulk Testing")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bulk Testing Options
                    if (bulkTestingMode) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Bulk Testing Options",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text("Number of Teachers: $numberOfTeachers")
                                Slider(
                                    value = numberOfTeachers.toFloat(),
                                    onValueChange = { numberOfTeachers = it.toInt() },
                                    valueRange = 2f..10f,
                                    steps = 8
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text("Select Day:")
                                Column {
                                    days.forEach { day ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedDay == day,
                                                onClick = { selectedDay = day }
                                            )
                                            Text(day.capitalize())
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    substituteManager.clearAssignments()
                                    
                                    if (bulkTestingMode) {
                                        // Run bulk test with multiple teachers
                                        val selectedTeachers = mutableListOf<Teacher>()
                                        val allResults = mutableMapOf<String, List<SubstituteAssignment>>()
                                        var totalAssignments = 0
                                        
                                        // Get multiple random teachers
                                        repeat(numberOfTeachers) {
                                            val teacher = substituteManager.getRandomTeacher()
                                            if (teacher != null && !selectedTeachers.contains(teacher)) {
                                                selectedTeachers.add(teacher)
                                            }
                                        }
                                        
                                        // Assign substitutes for each teacher
                                        for (teacher in selectedTeachers) {
                                            val teacherAssignments = substituteManager.assignSubstitutes(teacher.name, selectedDay)
                                            allResults[teacher.name] = teacherAssignments
                                            totalAssignments += teacherAssignments.size
                                        }
                                        
                                        // Save results to file
                                        val resultsJson = saveResultsToFile(allResults, selectedDay)
                                        
                                        bulkTestResults = allResults
                                        bulkTestResultMessage = "Processed ${selectedTeachers.size} teachers on $selectedDay with $totalAssignments total assignments.\nResults saved to: $resultsJson"
                                        bulkTestCompleted = true
                                    } else {
                                        // Single teacher test
                                        val teacher = substituteManager.getRandomTeacher()
                                        selectedTeacher = teacher
                                        
                                        if (teacher != null) {
                                            assignments = substituteManager.assignSubstitutes(teacher.name, "saturday")
                                        } else {
                                            errorMessage = "No teachers found"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error running algorithm: ${e.message}"
                                    Log.e("AlgorithmTestingScreen", "Error running algorithm: ${e.message}", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = dataLoaded,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (bulkTestingMode) "Run Bulk Test" else "Test Single Teacher")
                    }
                    
                    if (!dataLoaded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Please wait, data is being loaded...")
                    }
                }
            }
        }
    }
}

@Composable
fun BulkTestResults(
    results: Map<String, List<SubstituteAssignment>>,
    resultMessage: String,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bulk Test Results",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = resultMessage,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Teachers Tested (${results.size}):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(results.entries.toList()) { (teacherName, teacherAssignments) ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Teacher: $teacherName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Assignments: ${teacherAssignments.size}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        if (teacherAssignments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            teacherAssignments.groupBy { it.period }.forEach { (period, assignmentsForPeriod) ->
                                Text(
                                    text = "Period $period: ${assignmentsForPeriod.size} substitutes",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onReset,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Run Another Test")
        }
    }
}

@Composable
fun AssignmentResults(
    assignments: List<SubstituteAssignment>,
    selectedTeacher: Teacher?,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Algorithm Test Results",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        selectedTeacher?.let {
            Text(
                text = "Absent Teacher: ${it.name}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Day: Saturday",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (assignments.isEmpty()) {
            Text(
                text = "No assignments needed for this teacher on Saturday",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                text = "Assigned Substitutes (${assignments.size}):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(assignments.sortedBy { it.period }) { assignment ->
                    AssignmentCard(assignment)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onReset,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Test Another Teacher")
        }
    }
}

@Composable
fun AssignmentCard(assignment: SubstituteAssignment) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Period ${assignment.period}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Class: ${assignment.className}",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = "Substitute: ${assignment.substitute}",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = "Phone: ${assignment.substitutePhone}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Helper function to save test results to a file
private fun saveResultsToFile(results: Map<String, List<SubstituteAssignment>>, day: String): String {
    val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
    val testResultsDir = File(baseDir, "test_results")
    testResultsDir.mkdirs()
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    val fileName = "bulk_test_${day}_${dateFormat.format(Date())}.json"
    val file = File(testResultsDir, fileName)
    
    val resultData = mapOf(
        "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
        "day" to day,
        "teacherCount" to results.size,
        "results" to results
    )
    
    val gson = GsonBuilder().setPrettyPrinting().create()
    file.writeText(gson.toJson(resultData))
    
    return file.absolutePath
}

// Helper to capitalize the first letter of a string
private fun String.capitalize(): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
    }
} 