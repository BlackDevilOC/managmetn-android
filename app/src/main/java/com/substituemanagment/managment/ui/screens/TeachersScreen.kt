package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.substituemanagment.managment.data.TeacherData
import com.substituemanagment.managment.data.TeacherDataManager
import com.substituemanagment.managment.ui.viewmodels.TeachersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen() {
    val context = LocalContext.current
    val teacherDataManager = remember { TeacherDataManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var teachers by remember { mutableStateOf<List<TeacherData>>(emptyList()) }
    var absentTeacherNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    
    // Function to load all data
    fun loadData() {
        isLoading = true
        try {
            teachers = teacherDataManager.getAllTeachers()
            absentTeacherNames = teacherDataManager.getAbsentTeachers().map { it.name }.toSet()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        loadData()
    }

    // Filter teachers based on search query
    val filteredTeachers = remember(teachers, searchQuery, absentTeacherNames) {
        if (searchQuery.text.isEmpty()) {
            teachers
        } else {
            val query = searchQuery.text.lowercase()
            teachers.filter { 
                it.name.lowercase().contains(query) ||
                it.variations.any { variation -> variation.lowercase().contains(query) }
            }
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teachers") },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search teachers") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary of teacher statuses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TeacherStatusChip(
                    count = teachers.size - absentTeacherNames.size,
                    label = "Present",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                TeacherStatusChip(
                    count = absentTeacherNames.size,
                    label = "Absent",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredTeachers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.text.isEmpty()) 
                            "No teachers found" 
                        else 
                            "No teachers match '${searchQuery.text}'",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredTeachers,
                        key = { teacher -> teacher.name } // Use name as stable key
                    ) { teacher ->
                        val isAbsent = absentTeacherNames.contains(teacher.name)
                        TeacherCard(
                            teacher = teacher,
                            isAbsent = isAbsent,
                            onMarkAbsent = { 
                                coroutineScope.launch {
                                    if (!isAbsent) {
                                        teacherDataManager.markTeacherAbsent(teacher.name)
                                        snackbarMessage = "${teacher.name} marked as absent"
                                        showSnackbar = true
                                        // Refresh data to update UI
                                        loadData()
                                    }
                                }
                            },
                            onMarkPresent = {
                                coroutineScope.launch {
                                    if (isAbsent) {
                                        teacherDataManager.markTeacherPresent(teacher.name)
                                        snackbarMessage = "${teacher.name} marked as present"
                                        showSnackbar = true
                                        // Refresh data to update UI
                                        loadData()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherStatusChip(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TeacherCard(
    teacher: TeacherData,
    isAbsent: Boolean,
    onMarkAbsent: () -> Unit,
    onMarkPresent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAbsent) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = teacher.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (teacher.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = teacher.phone,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAbsent) Icons.Default.Close else Icons.Default.Check,
                        contentDescription = if (isAbsent) "Absent" else "Present",
                        tint = if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAbsent) "Absent" else "Present",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (isAbsent) {
                FilledTonalButton(
                    onClick = onMarkPresent,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Mark Present",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Present")
                }
            } else {
                FilledTonalButton(
                    onClick = onMarkAbsent,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = "Mark Absent",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Absent")
                }
            }
        }
    }
} 