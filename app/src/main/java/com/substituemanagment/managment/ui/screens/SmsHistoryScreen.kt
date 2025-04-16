package com.substituemanagment.managment.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsHistoryScreen(
    navController: NavController,
    viewModel: SmsViewModel = viewModel()
) {
    val smsHistory = viewModel.smsHistory
    var showResponseDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS History") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (smsHistory.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No SMS History",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Messages you send will appear here",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(smsHistory) { sms ->
                        SmsHistoryCard(
                            sms = sms,
                            onResponseClick = { showResponseDialog = sms.id }
                        )
                    }
                }
            }
        }
    }

    // Response Dialog
    showResponseDialog?.let { smsId ->
        val sms = smsHistory.find { it.id == smsId }
        if (sms != null) {
            AlertDialog(
                onDismissRequest = { showResponseDialog = null },
                title = { Text("SMS Response") },
                text = {
                    Column {
                        Text("Teacher: ${sms.teacherName}")
                        Text("Phone: ${sms.teacherPhone}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Message: ${sms.message}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Status: ${sms.status}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showResponseDialog = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsHistoryCard(
    sms: SmsViewModel.SmsMessage,
    onResponseClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = sms.teacherName,
                    style = MaterialTheme.typography.titleMedium
                )
                
                StatusChip(status = sms.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = sms.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(sms.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TextButton(
                    onClick = onResponseClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "View Response"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Response")
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, contentColor) = when (status.lowercase()) {
        "sent" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "failed" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        "delivered" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
    }
    
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
} 