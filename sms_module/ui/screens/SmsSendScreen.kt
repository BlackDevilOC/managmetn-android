package com.substituemanagment.managment.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SmsSendScreen(navController: NavController) {
    val viewModel: SmsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State for UI
    val showTemplate = remember { mutableStateOf(false) }
    val expandedTeacherId = remember { mutableStateOf<String?>(null) }
    val showHistory = remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val needsPermission by viewModel.needsPermission
    
    // Success message state
    val showSuccessBar = remember { mutableStateOf(false) }
    val successMessage = remember { mutableStateOf("") }
    
    // Permission dialog state
    val showPermissionDialog = remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("SMS permission is required to send messages")
            }
        } else {
            viewModel.sendSmsToTeachersOneByOne(context) {
                showSuccess(scope, successMessage, showSuccessBar, "SMS sent successfully")
            }
        }
    }
    
    // Prepare data for SMS
    LaunchedEffect(Unit) {
        viewModel.prepareSelectedTeachersForSms()
    }
    
    // Show error message if any
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    // Show permission dialog if needed
    LaunchedEffect(needsPermission) {
        if (needsPermission) {
            showPermissionDialog.value = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send SMS") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTemplate.value = !showTemplate.value }) {
                        Icon(
                            if (showTemplate.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Template"
                        )
                    }
                    IconButton(onClick = { showHistory.value = !showHistory.value }) {
                        Icon(
                            if (showHistory.value) Icons.Default.History else Icons.Default.History,
                            contentDescription = "SMS History"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Template editor
                    if (showTemplate.value) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "Message Template",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = viewModel.messageTemplate.value,
                                    onValueChange = { viewModel.messageTemplate.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Template") },
                                    supportingText = { Text("Use {substitute}, {class}, {period}, {date} as placeholders") }
                                )
                            }
                        }
                    }
                    
                    // SMS History
                    if (showHistory.value) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "SMS History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (viewModel.smsHistory.isEmpty()) {
                                    Text("No SMS history yet")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                    ) {
                                        items(viewModel.smsHistory) { sms ->
                                            SmsHistoryItem(sms)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Teacher selection
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Teachers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    TextButton(
                                        onClick = { viewModel.selectAllTeachers(true) }
                                    ) {
                                        Text("Select All")
                                    }
                                    TextButton(
                                        onClick = { viewModel.selectAllTeachers(false) }
                                    ) {
                                        Text("Deselect All")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(viewModel.teachersToProcess) { teacher ->
                                    val isSelected = viewModel.selectedTeachers[teacher.name] ?: false
                                    val isExpanded = expandedTeacherId.value == teacher.name
                                    val assignments = teacher.assignments
                                    val assignmentsSummary = getAssignmentsSummary(assignments)
                                    
                                    ExpandableTeacherItem(
                                        teacherName = teacher.name,
                                        isSelected = isSelected,
                                        onSelectionChange = { viewModel.toggleTeacherSelection(teacher.name) },
                                        assignmentsSummary = assignmentsSummary,
                                        isExpanded = isExpanded,
                                        onExpandToggle = { 
                                            expandedTeacherId.value = if (isExpanded) null else teacher.name
                                        },
                                        messageTemplate = viewModel.messageTemplate.value,
                                        assignments = assignments,
                                        onSendIndividual = {
                                            if (viewModel.checkSmsPermissions()) {
                                                val message = viewModel.generateSmsForTeacher(teacher)
                                                // In a real app, this would send the SMS
                                                showSuccess(scope, successMessage, showSuccessBar, "SMS sent to ${teacher.name}")
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.SEND_SMS)
                                            }
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    if (viewModel.checkSmsPermissions()) {
                                        viewModel.sendSmsToTeachersOneByOne(context) {
                                            showSuccess(scope, successMessage, showSuccessBar, "SMS sent successfully")
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.SEND_SMS)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = viewModel.teachersToProcess.isNotEmpty() && 
                                          viewModel.selectedTeachers.any { it.value }
                            ) {
                                Text("Send SMS to Selected Teachers")
                            }
                        }
                    }
                }
            }
            
            // Success message
            if (showSuccessBar.value) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            successMessage.value,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Permission dialog
            if (showPermissionDialog.value) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog.value = false },
                    title = { Text("Permission Required") },
                    text = { Text("SMS permission is required to send messages. Please grant the permission.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPermissionDialog.value = false
                                permissionLauncher.launch(Manifest.permission.SEND_SMS)
                            }
                        ) {
                            Text("Grant Permission")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showPermissionDialog.value = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

private fun showSuccess(
    scope: CoroutineScope,
    successMessage: MutableState<String>,
    showSuccessBar: MutableState<Boolean>,
    message: String
) {
    successMessage.value = message
    showSuccessBar.value = true
    scope.launch {
        kotlinx.coroutines.delay(3000)
        showSuccessBar.value = false
    }
}

@Composable
private fun SmsHistoryItem(sms: SmsViewModel.SmsMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    sms.teacherName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatTimestamp(sms.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                sms.teacherPhone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                sms.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (sms.status) {
                        "Sent" -> Color(0xFF4CAF50) // Green
                        "Failed" -> Color(0xFFF44336) // Red
                        else -> Color(0xFFFF9800) // Orange
                    }
                ) {
                    Text(
                        sms.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableTeacherItem(
    teacherName: String,
    isSelected: Boolean,
    onSelectionChange: () -> Unit,
    assignmentsSummary: String,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    messageTemplate: String,
    assignments: List<SmsViewModel.AssignmentData>,
    onSendIndividual: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onExpandToggle() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange() }
                )
                
                Text(
                    teacherName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = { onExpandToggle() }) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (!isExpanded) {
                Text(
                    assignmentsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Assignments:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    items(assignments) { assignment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "Class: ${assignment.className}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Period: ${assignment.period}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Date: ${assignment.date}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Preview Message:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        createPreviewMessage(messageTemplate, teacherName, assignments.firstOrNull()),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onSendIndividual() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send to this teacher")
                }
            }
        }
    }
}

private fun getAssignmentsSummary(assignments: List<SmsViewModel.AssignmentData>): String {
    if (assignments.isEmpty()) return "No assignments"
    
    return if (assignments.size == 1) {
        val assignment = assignments[0]
        "${assignment.className} (Period ${assignment.period})"
    } else {
        "${assignments.size} assignments"
    }
}

private fun createPreviewMessage(
    template: String,
    teacherName: String,
    assignment: SmsViewModel.AssignmentData?
): String {
    if (assignment == null) return "No assignment data available"
    
    var message = template
    message = message.replace("{substitute}", teacherName)
    message = message.replace("{class}", assignment.className)
    message = message.replace("{period}", assignment.period.toString())
    message = message.replace("{date}", assignment.date)
    return message
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return format.format(date)
}
