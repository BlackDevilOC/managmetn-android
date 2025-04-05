package com.substituemanagment.managment.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
    
    var showTeacherSelection by remember { mutableStateOf(false) }
    var showTemplateSelection by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    val isLoading by remember { viewModel.isLoading }
    val errorMessage by remember { viewModel.errorMessage }
    val needsPermission by remember { viewModel.needsPermission }
    
    // For snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccessBar by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // For permission explanation dialog
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Permission request handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted, proceed with sending SMS
                viewModel.sendSms()
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
    
    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("SMS Permission Required") },
            text = { 
                Column {
                    Text(
                        "This app needs permission to send SMS messages directly from your device to notify teachers about substitution assignments."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Standard SMS rates from your carrier will apply.",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        permissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
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
    
    // Effect to show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    // Function to show success message
    fun showSuccess(message: String) {
        successMessage = message
        showSuccessBar = true
        scope.launch {
            delay(3000)
            showSuccessBar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send SMS") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "SMS History"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Show loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            }
            
            // Success bar
            AnimatedVisibility(
                visible = showSuccessBar,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
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
            
            // Main SMS Compose Screen
            if (!showHistory) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SMS Notifications",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Recipients Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Recipients",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${viewModel.teachers.count { it.selected }} teachers selected",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                TextButton(
                                    onClick = { showTeacherSelection = !showTeacherSelection }
                                ) {
                                    Text("Select Teachers")
                                    Icon(
                                        imageVector = if (showTeacherSelection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand"
                                    )
                                }
                            }
                            
                            // Using AnimatedVisibility to show/hide teacher selection
                            AnimatedVisibility(
                                visible = showTeacherSelection,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { viewModel.selectAllTeachers(true) }
                                        ) {
                                            Text("Select All")
                                        }
                                        
                                        TextButton(
                                            onClick = { viewModel.selectAllTeachers(false) }
                                        ) {
                                            Text("Clear All")
                                        }
                                    }
                                    
                                    viewModel.teachers.forEach { teacher ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = teacher.selected,
                                                onCheckedChange = { viewModel.toggleTeacherSelection(teacher.id) }
                                            )
                                            
                                            Text(
                                                text = "${teacher.name} (${teacher.phone})",
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { viewModel.toggleTeacherSelection(teacher.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Message Template Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Message",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.selectedTemplate.value?.name ?: "Custom Message",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                TextButton(
                                    onClick = { showTemplateSelection = !showTemplateSelection }
                                ) {
                                    Text("Templates")
                                    Icon(
                                        imageVector = if (showTemplateSelection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand"
                                    )
                                }
                            }
                            
                            // Using AnimatedVisibility to show/hide template selection
                            AnimatedVisibility(
                                visible = showTemplateSelection,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    viewModel.templates.forEach { template ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (viewModel.selectedTemplate.value?.id == template.id)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        Color.Transparent
                                                )
                                                .clickable {
                                                    viewModel.selectTemplate(template.id)
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = template.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = template.content,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2
                                                )
                                            }
                                            
                                            if (viewModel.selectedTemplate.value?.id == template.id) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (viewModel.selectedTemplate.value == null)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else
                                                    Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.selectTemplate(null)
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Custom Message",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        if (viewModel.selectedTemplate.value == null) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = viewModel.customMessage.value,
                                onValueChange = { viewModel.customMessage.value = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                placeholder = { Text("Enter your message here") },
                                label = { Text("Message Content") }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val messageLength = viewModel.customMessage.value.length
                            val messageCount = (messageLength / 160) + if (messageLength % 160 > 0) 1 else 0
                            
                            Text(
                                text = "Character count: $messageLength / Message count: $messageCount",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                                color = if (messageLength > 160) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Available variables: {date}, {day}, {time}, {period}, {class}, {substitute_teacher}, {original_teacher}, {room}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    
                    // New Template Card (Add/Edit templates)
                    var showNewTemplateDialog by remember { mutableStateOf(false) }
                    var newTemplateName by remember { mutableStateOf("") }
                    var newTemplateContent by remember { mutableStateOf("") }
                    
                    if (showNewTemplateDialog) {
                        AlertDialog(
                            onDismissRequest = { showNewTemplateDialog = false },
                            title = { Text("Add Template") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = newTemplateName,
                                        onValueChange = { newTemplateName = it },
                                        label = { Text("Template Name") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedTextField(
                                        value = newTemplateContent,
                                        onValueChange = { newTemplateContent = it },
                                        label = { Text("Template Content") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                    
                                    // Character count
                                    val templateLength = newTemplateContent.length
                                    val templateCount = (templateLength / 160) + if (templateLength % 160 > 0) 1 else 0
                                    
                                    Text(
                                        text = "Character count: $templateLength / Message count: $templateCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        color = if (templateLength > 160) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newTemplateName.isNotBlank() && newTemplateContent.isNotBlank()) {
                                            viewModel.saveNewTemplate(newTemplateName, newTemplateContent)
                                            showSuccess("Template saved successfully")
                                            showNewTemplateDialog = false
                                            newTemplateName = ""
                                            newTemplateContent = ""
                                        }
                                    },
                                    enabled = newTemplateName.isNotBlank() && newTemplateContent.isNotBlank()
                                ) {
                                    Text("Save")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showNewTemplateDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    
                    // Add New Template Button
                    OutlinedButton(
                        onClick = { showNewTemplateDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Template"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Message Template")
                    }
                    
                    // Send Button
                    Button(
                        onClick = { 
                            if (viewModel.checkSmsPermissions()) {
                                viewModel.sendSms()
                                scope.launch {
                                    delay(500) // Wait for the SMS to be sent
                                    if (viewModel.errorMessage.value == null && !viewModel.needsPermission.value) {
                                        showSuccess("SMS sent successfully")
                                    }
                                }
                            } else {
                                showPermissionDialog = true
                            }
                        },
                        enabled = viewModel.customMessage.value.isNotBlank() && viewModel.teachers.any { it.selected } && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.Default.Send, 
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send SMS Notifications")
                    }
                    
                    // SMS sending info
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "SMS Sending Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "• Messages are sent using your device's SIM card and carrier rates apply",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "• Standard SMS messages can contain up to 160 characters",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "• Longer messages will be split into multiple SMS messages",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "• Delivery status depends on your network carrier and may not be 100% accurate",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "• SMS permission is required for this feature to work",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "• Ensure your device has sufficient balance/credit for sending SMS",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // SMS History Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SMS History",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (viewModel.smsHistory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No SMS history found",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.smsHistory) { sms ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                            
                                            // Custom Chip implementation for SMS status
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp)),
                                                color = when (sms.status) {
                                                    "Sent" -> MaterialTheme.colorScheme.primaryContainer
                                                    "Failed" -> MaterialTheme.colorScheme.errorContainer
                                                    "Partial" -> MaterialTheme.colorScheme.tertiaryContainer
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            ) {
                                                Text(
                                                    text = sms.status,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = when (sms.status) {
                                                        "Sent" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        "Failed" -> MaterialTheme.colorScheme.onErrorContainer
                                                        "Partial" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
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
                                            text = "Recipients: ${sms.recipients.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall
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
} 