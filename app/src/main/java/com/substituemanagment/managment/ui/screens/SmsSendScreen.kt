package com.substituemanagment.managment.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSendScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val viewModel: SmsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showHistory by remember { mutableStateOf(false) }
    var expandedTeacherId by remember { mutableStateOf<String?>(null) }
    var showTemplate by remember { mutableStateOf(false) }
    
    val isLoading by remember { viewModel.isLoading }
    val errorMessage by remember { viewModel.errorMessage }
    val needsPermission by remember { viewModel.needsPermission }
    
    // For snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccessBar by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // For permission explanation dialog
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Function to show success message
    fun showSuccess(message: String) {
        successMessage = message
        showSuccessBar = true
        scope.launch {
            delay(3000)
            showSuccessBar = false
        }
    }
    
    // Permission request handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted, proceed with sending SMS
                viewModel.sendSmsToTeachersOneByOne(context) {
                    showSuccess("SMS sent successfully")
                }
            } else {
                // Permission denied, show error message
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "SMS permission denied. Cannot send messages.",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    )
    
    // Show permission dialog if needed
    LaunchedEffect(needsPermission) {
        if (needsPermission) {
            showPermissionDialog = true
        }
    }
    
    // Effect to show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Toggle history/compose view
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(
                            imageVector = if (showHistory) Icons.Default.Edit else Icons.Default.History,
                            contentDescription = if (showHistory) "Compose SMS" else "SMS History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Send to all button
                if (!showHistory && viewModel.teacherAssignments.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (viewModel.selectedTeachers.values.any { it }) {
                                Log.d("SmsSendScreen", "Sending to selected teachers")
                                
                                // Clear existing prepared teachers
                                viewModel.teachersToProcess.clear()
                                
                                // Force refresh selection
                                viewModel.prepareSelectedTeachersForSms()
                                
                                Log.d("SmsSendScreen", "Teachers prepared: ${viewModel.teachersToProcess.size}")
                                
                                // Make sure we have teachers to process before navigating
                                if (viewModel.teachersToProcess.isNotEmpty()) {
                                    // Navigate to process screen
                                    navController.navigate(Screen.SmsProcess.route)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Error preparing teachers. Please try again.",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Please select at least one teacher",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Default.Send, contentDescription = "Send to Selected") },
                        text = { Text("Send to Selected") }
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
                Box(
                    modifier = Modifier
                    .padding(paddingValues)
                        .fillMaxSize()
        ) {
            if (showHistory) {
                // History UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                ) {
                    Text(
                        text = "SMS History",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (viewModel.smsHistory.isEmpty()) {
                        // Empty history state
                            Column(
                    modifier = Modifier
                        .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No History",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                        Text(
                                text = "No SMS History Found",
                                style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                text = "When you send SMS notifications, they will appear here.",
                                style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // SMS history list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.smsHistory) { sms ->
                                SmsHistoryItem(sms = sms)
                            }
                        }
                    }
                }
            } else {
                // Compose SMS UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                ) {
                    if (viewModel.teacherAssignments.isEmpty()) {
                        // No assigned substitute teachers found
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                .fillMaxHeight(0.9f)
                                    .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                                Text(
                                text = "No Assigned Substitute Teachers Found",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                
                            Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                text = "There are currently no assigned substitute teachers in the system. " +
                                      "Please make sure you have processed substitute assignments first.",
                                style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            OutlinedCard(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                    .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                        text = "Troubleshooting",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                    Text(
                                        text = "The app is looking for assigned substitutes at:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                                    Text(
                                        text = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data/assigned_substitute.json",
                                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                            .padding(8.dp)
                                    )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                        text = "To fix this issue:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Text(
                                        text = "1. Make sure you have processed assignments\n" +
                                              "2. Check that the file exists at the location above\n" +
                                              "3. Try refreshing the data using the refresh button",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                Button(
                                    onClick = { navController.navigate(Screen.Process.route) },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Assignments")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Go to Assignments")
                                }
                                
                                Button(
                                    onClick = { viewModel.refreshData() },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            Spacer(modifier = Modifier.width(8.dp))
                                    Text("Refresh")
                                }
                                        }
                                    }
                                } else {
                        // Redesigned SMS compose UI
                                    Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Message Template Editor - Collapsible Section
                            Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clickable { showTemplate = !showTemplate },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Message,
                                            contentDescription = "Message Template",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                            text = "Message Template",
                                            style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                
                                                    Icon(
                                        imageVector = if (showTemplate) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (showTemplate) "Hide Template" else "Show Template",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                }
                                
                                AnimatedVisibility(
                                    visible = showTemplate,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    ) {
                                OutlinedTextField(
                                            value = viewModel.messageTemplate.value,
                                            onValueChange = { viewModel.messageTemplate.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                            label = { Text("Message Content") },
                                            maxLines = 4,
                                            minLines = 3
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                            text = "Available Variables: {substitute}, {class}, {period}, {date}, {day}, {original_teacher}",
                                    style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // Select/Clear All Buttons - Horizontally aligned
                            Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                    .padding(bottom = 12.dp, top = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Select/Clear buttons
                                Button(
                                    onClick = { viewModel.selectAllTeachers(true) },
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                        ) {
                            Icon(
                                        Icons.Default.CheckCircle, 
                                        contentDescription = "Select All",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Select All")
                                }
                                
                                OutlinedButton(
                                    onClick = { viewModel.selectAllTeachers(false) },
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                        Icons.Default.Cancel, 
                                        contentDescription = "Clear All",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All")
                                }
                            }
                            
                            // Teacher list with expandable details
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(viewModel.teacherAssignments.keys.toList()) { teacherName ->
                                    val isSelected = viewModel.selectedTeachers[teacherName] ?: false
                                    val assignments = viewModel.teacherAssignments[teacherName] ?: emptyList()
                                    val isExpanded = expandedTeacherId == teacherName
                                    
                                    ExpandableTeacherItem(
                                        teacherName = teacherName,
                                        isSelected = isSelected,
                                        onSelectionChange = { viewModel.toggleTeacherSelection(teacherName) },
                                        assignmentsSummary = getAssignmentsSummary(assignments),
                                        isExpanded = isExpanded,
                                        onExpandToggle = {
                                            expandedTeacherId = if (isExpanded) null else teacherName
                                        },
                                        messageTemplate = viewModel.messageTemplate.value,
                                        assignments = assignments,
                                        onSendIndividual = {
                                            // Clear all selections
                                            viewModel.selectAllTeachers(false)
                                            
                                            // Select only this teacher
                                            viewModel.toggleTeacherSelection(teacherName)
                                            
                                            Log.d("SmsSendScreen", "Sending individually to: $teacherName")
                                            
                                            // Force clear teachers to process
                                            viewModel.teachersToProcess.clear()
                                            
                                            // Prepare the selected teachers and save to file
                                            viewModel.prepareSelectedTeachersForSms()
                                            
                                            Log.d("SmsSendScreen", "Teachers prepared for individual: ${viewModel.teachersToProcess.size}")
                                            
                                            // Make sure we have at least one teacher to process
                                            if (viewModel.teachersToProcess.isNotEmpty()) {
                                                // Navigate to process screen
                                                navController.navigate(Screen.SmsProcess.route)
                } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Error preparing teacher. Please try again.",
                                                        duration = SnackbarDuration.Short
                                                    )
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
            
            // Permission explanation dialog
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    icon = { Icon(Icons.Default.Message, contentDescription = null) },
                    title = { Text("SMS Permission Required") },
                    text = { 
                        Column {
                            Text(
                                "This app needs permission to send SMS messages directly from your device to notify substitute teachers about their assignments."
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Standard SMS rates from your carrier will apply.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPermissionDialog = false
                                permissionLauncher.launch(Manifest.permission.SEND_SMS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Grant Permission")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { 
                                showPermissionDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "SMS permission is required to send messages.",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Success notification
            AnimatedVisibility(
                visible = showSuccessBar,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = successMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmsHistoryItem(sms: SmsViewModel.SmsMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
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
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(sms.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
                
                // Enhanced status chip
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp)),
                    color = when (sms.status) {
                        "Sent" -> MaterialTheme.colorScheme.primaryContainer
                        "Failed" -> MaterialTheme.colorScheme.errorContainer
                        "Partial" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = when (sms.status) {
                            "Sent" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            "Failed" -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            "Partial" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (sms.status) {
                                "Sent" -> Icons.Default.Check
                                "Failed" -> Icons.Default.Close
                                "Partial" -> Icons.Default.Info
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when (sms.status) {
                                "Sent" -> MaterialTheme.colorScheme.primary
                                "Failed" -> MaterialTheme.colorScheme.error
                                "Partial" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = sms.status,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = when (sms.status) {
                                "Sent" -> MaterialTheme.colorScheme.primary
                                "Failed" -> MaterialTheme.colorScheme.error
                                "Partial" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = sms.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Recipient: ${sms.teacherName}",
                style = MaterialTheme.typography.bodySmall
            )
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onExpandToggle() },
        tonalElevation = if (isExpanded) 4.dp else 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectionChange() }
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionChange() }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = teacherName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = assignmentsSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Expand/collapse icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Assignment details
                    Text(
                        text = "Assignment Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (assignments.isEmpty()) {
                        Text(
                            text = "No assignments found for this teacher",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            assignments.forEach { assignment ->
                                Text(
                                    text = "• Period ${assignment.period}: ${assignment.className} (for ${assignment.originalTeacher})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // SMS preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "SMS Preview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val previewMessage = if (assignments.isNotEmpty()) {
                                createPreviewMessage(messageTemplate, teacherName, assignments.first())
                            } else {
                                "No assignment data available for SMS preview"
                            }
                            
                            Text(
                                text = previewMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Send SMS button
                    Button(
                        onClick = onSendIndividual,
                        modifier = Modifier.align(Alignment.End),
                        enabled = messageTemplate.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send SMS to ${teacherName}")
                    }
                }
            }
        }
    }
}

// Helper function to create a preview message
private fun createPreviewMessage(
    template: String,
    teacherName: String,
    assignment: SmsViewModel.AssignmentData
): String {
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val today = Date()
    
    return template
        .replace("{substitute}", teacherName)
        .replace("{class}", assignment.className)
        .replace("{period}", assignment.period.toString())
        .replace("{date}", dateFormat.format(today))
        .replace("{day}", dayFormat.format(today))
        .replace("{original_teacher}", assignment.originalTeacher)
}

// Helper function to create a summary of assignments
private fun getAssignmentsSummary(assignments: List<SmsViewModel.AssignmentData>): String {
    if (assignments.isEmpty()) return "No assignments"
    
    val sortedAssignments = assignments.sortedBy { it.period }
    return if (sortedAssignments.size == 1) {
        val assignment = sortedAssignments.first()
        "Period ${assignment.period}: ${assignment.className} (for ${assignment.originalTeacher})"
    } else {
        "Assigned to ${sortedAssignments.size} classes: " + 
        sortedAssignments.joinToString(", ") { "P${it.period}" }
    }
} 