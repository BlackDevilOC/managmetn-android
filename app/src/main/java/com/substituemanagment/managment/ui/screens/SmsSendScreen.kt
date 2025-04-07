package com.substituemanagment.managment.ui.screens

import android.Manifest
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
                title = { Text("SMS Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
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
                            imageVector = if (showHistory) Icons.Default.Edit else Icons.Default.History,
                            contentDescription = if (showHistory) "Compose SMS" else "SMS History"
                        )
                    }
                }
            )
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
                // ... existing code ...
            } else {
                // Compose SMS UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (viewModel.teachers.isEmpty()) {
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
                        // Regular SMS compose UI if teachers are found
                        // ... existing code ...
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
                text = "Recipients: ${sms.recipients.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
} 