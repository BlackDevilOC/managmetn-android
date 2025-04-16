package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsProcessScreen(
    navController: NavController,
    selectedTeachers: String?
) {
    val viewModel: SmsViewModel = viewModel()
    val teachersList = selectedTeachers?.split(",") ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process SMS") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (teachersList.isEmpty()) {
                // Show message if no teachers are selected
                Text(
                    text = "No teachers selected. Please go back and select teachers.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Display selected teachers info
                Text(
                    text = "Selected Teachers (${teachersList.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                teachersList.forEach { teacher ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = teacher,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Display assignments for this teacher
                            viewModel.teacherAssignments[teacher]?.let { assignments ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Assignments",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                assignments.forEach { assignment ->
                                    Text(
                                        text = "Period ${assignment.period}: ${assignment.className} (for ${assignment.originalTeacher})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 