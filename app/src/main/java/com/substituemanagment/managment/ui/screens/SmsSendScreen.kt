package com.substituemanagment.managment.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel.AssignmentData
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel.TeacherWithAssignments
import androidx.compose.foundation.clickable
import com.substituemanagment.managment.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSendScreen(navController: NavController) {
    val viewModel: SmsViewModel = viewModel()
    val messageTemplate = viewModel.messageTemplate.value
    val selectedTeachers = viewModel.selectedTeachers
    val teacherAssignments = viewModel.teacherAssignments
    var expanded by remember { mutableStateOf(false) }
    var currentlyExpandedKey by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Effect to load assignments when screen is created
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(
                        onClick = { navController.navigate("sms_history") }
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show FAB if at least one teacher is selected
            if (selectedTeachers.any { it.value }) {
            FloatingActionButton(
                    onClick = {
                        // Get all selected teachers
                        val selectedTeachersList = selectedTeachers
                            .filter { it.value }
                            .map { it.key }
                            .joinToString(",")
                        
                        // Navigate to SMS Process screen with all selected teachers
                        navController.navigate(
                            "${Screen.SmsProcess.route}/$selectedTeachersList"
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Process SMS"
                )
                        Text("Process SMS")
                    }
                }
            }
        }
    ) { paddingValues ->
        if (viewModel.isLoading.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Message Template Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { expanded = !expanded }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Message,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Message Template",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (expanded) "Collapse" else "Expand"
                                )
                            }
                            
                            if (expanded) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = messageTemplate,
                                    onValueChange = { viewModel.messageTemplate.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Message Template") },
                                    minLines = 3
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Available Variables: {substitute}, {class}, {period}, {date}, {day}, {original_teacher}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.selectAllTeachers(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Text("Select All")
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.selectAllTeachers(false) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(MaterialTheme.colorScheme.error)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Text("Clear All")
                            }
                        }
                    }
                }

                // Teacher Assignment Cards
                items(teacherAssignments.entries.toList()) { (teacher, assignments) ->
                    assignments.forEach { assignment ->
                        val assignmentKey = "${teacher}-${assignment.period}-${assignment.className}"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTeachers[teacher] == true) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp,
                                focusedElevation = 4.dp
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selectedTeachers[teacher] == true)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            currentlyExpandedKey = if (currentlyExpandedKey == assignmentKey) {
                                                null
                                            } else {
                                                assignmentKey
                                            }
                                        }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Checkbox(
                                            checked = selectedTeachers[teacher] == true,
                                            onCheckedChange = { isChecked ->
                                                viewModel.toggleTeacherSelection(teacher)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                uncheckedColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        Column {
                                            Text(
                                                text = teacher,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTeachers[teacher] == true)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Period ${assignment.period}: ${assignment.className} (for ${assignment.originalTeacher})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (selectedTeachers[teacher] == true)
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (currentlyExpandedKey == assignmentKey) 
                                            Icons.Default.ExpandLess 
                                        else 
                                            Icons.Default.ExpandMore,
                                        contentDescription = "Toggle details",
                                        tint = if (selectedTeachers[teacher] == true)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (currentlyExpandedKey == assignmentKey) {
                                    HorizontalDivider(
                                        color = if (selectedTeachers[teacher] == true)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
        ) {
            Text(
                                            text = "SMS Preview",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTeachers[teacher] == true)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = messageTemplate
                                                .replace("{substitute}", teacher)
                                                .replace("{class}", assignment.className)
                                                .replace("{period}", assignment.period.toString())
                                                .replace("{date}", assignment.date)
                                                .replace("{day}", "Wednesday")
                                                .replace("{original_teacher}", assignment.originalTeacher),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selectedTeachers[teacher] == true)
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
} 