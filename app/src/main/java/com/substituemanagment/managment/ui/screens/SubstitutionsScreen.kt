package com.substituemanagment.managment.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
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
import com.substituemanagment.managment.data.AssignmentRecord
import com.substituemanagment.managment.data.TeacherDataManager
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionsScreen(
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    val teacherDataManager = remember { TeacherDataManager(context) }
    var assignmentRecords by remember { mutableStateOf<List<AssignmentRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Function to load assignments data
    fun loadAssignmentData() {
        isLoading = true
        try {
            assignmentRecords = teacherDataManager.getAssignmentRecords()
            Log.d("SubstitutionsScreen", "Loaded ${assignmentRecords.size} assignment records")
            
            // Sort assignments by date (newest first)
            assignmentRecords = assignmentRecords.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e("SubstitutionsScreen", "Error loading assignment records: ${e.message}")
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        loadAssignmentData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assigned Substitutes") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back to Absences")
                    }
                },
                actions = {
                    IconButton(onClick = { loadAssignmentData() }) {
                        Icon(Icons.Default.Refresh, "Refresh Data")
                    }
                }
            )
        }
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
                // Header Section
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
                            text = "Substitution Assignments",
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
                    }
                }

                // Display assignment records
                if (assignmentRecords.isEmpty()) {
                    EmptyAssignmentsMessage("No Assignments Found", 
                        "There are currently no substitute assignments. Create assignments in the Manage Absences screen.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(assignmentRecords) { record ->
                            AssignmentRecordCard(record = record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyAssignmentsMessage(title: String, message: String) {
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AssignmentRecordCard(record: AssignmentRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Assignment Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assignment Record",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = record.assignmentDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Teacher Info
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Absent Teacher",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = record.absentTeacher,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Substitute Teacher",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = record.substituteTeacher,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (record.substitutePhone.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Phone",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.substitutePhone,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            // Periods and Classes
            Text(
                text = "Classes & Periods:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (record.periods.isEmpty()) {
                    Text(
                        text = "No specific periods assigned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    record.periods.sortedBy { it.period }.forEach { assignment ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Period Info",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Period ${assignment.period} - ${assignment.className}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Timestamp as relative time
            val relativeTime = remember(record.timestamp) {
                try {
                    val instant = Instant.parse(record.timestamp)
                    val now = Instant.now()
                    val diff = java.time.Duration.between(instant, now)
                    
                    when {
                        diff.toMinutes() < 60 -> "${diff.toMinutes()} minutes ago"
                        diff.toHours() < 24 -> "${diff.toHours()} hours ago"
                        else -> "${diff.toDays()} days ago"
                    }
                } catch (e: Exception) {
                    "Recently"
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Created: $relativeTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
} 