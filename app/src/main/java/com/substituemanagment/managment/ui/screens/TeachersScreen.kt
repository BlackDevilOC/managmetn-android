package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.substituemanagment.managment.ui.viewmodels.TeachersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(
    viewModel: TeachersViewModel = viewModel()
) {
    val context = LocalContext.current
    val teachers by viewModel.teachers.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teachers") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Teacher Name",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(100.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Teachers List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(teachers) { teacher ->
                    TeacherItem(
                        teacher = teacher,
                        onToggleAttendance = { viewModel.toggleTeacherAttendance(teacher.name) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherItem(
    teacher: com.substituemanagment.managment.ui.viewmodels.Teacher,
    onToggleAttendance: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onToggleAttendance
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = teacher.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (teacher.isAbsent) Icons.Default.Close else Icons.Default.Check,
                contentDescription = if (teacher.isAbsent) "Absent" else "Present",
                tint = if (teacher.isAbsent) Color.Red else Color.Green,
                modifier = Modifier.width(24.dp)
            )
        }
    }
} 