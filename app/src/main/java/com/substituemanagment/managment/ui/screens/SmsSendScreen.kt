package com.substituemanagment.managment.ui.screens

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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSendScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val viewModel: SmsViewModel = viewModel()
    val context = LocalContext.current
    
    var showTeacherSelection by remember { mutableStateOf(false) }
    var showTemplateSelection by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    val isLoading by remember { viewModel.isLoading }
    val errorMessage by remember { viewModel.errorMessage }
    
    // Effect to show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // In a real app, you would show a Snackbar or Dialog here
            println("Error: $it")
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
                            
                            Text(
                                text = "Character count: ${viewModel.customMessage.value.length} / Message count: ${(viewModel.customMessage.value.length / 160) + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Variables: {date}, {time}, {period} - Will be replaced with actual values when sent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newTemplateName.isNotBlank() && newTemplateContent.isNotBlank()) {
                                            viewModel.saveNewTemplate(newTemplateName, newTemplateContent)
                                            showNewTemplateDialog = false
                                            newTemplateName = ""
                                            newTemplateContent = ""
                                        }
                                    }
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
                        onClick = { viewModel.sendSms() },
                        enabled = viewModel.customMessage.value.isNotBlank() && viewModel.teachers.any { it.selected },
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
                                            
                                            // Custom Chip implementation instead of the built-in Chip
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(
                                                        when (sms.status) {
                                                            "Sent" -> MaterialTheme.colorScheme.primaryContainer
                                                            "Failed" -> MaterialTheme.colorScheme.errorContainer
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        }
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = Color.Transparent
                                            ) {
                                                Text(
                                                    text = sms.status,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = when (sms.status) {
                                                        "Sent" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        "Failed" -> MaterialTheme.colorScheme.onErrorContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
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