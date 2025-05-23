package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.substituemanagment.managment.ui.viewmodels.ProcessUiState
import com.substituemanagment.managment.ui.viewmodels.ProcessViewModel
import com.substituemanagment.managment.ui.viewmodels.TeacherDetailsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessScreen(navController: NavController = rememberNavController()) {
    val viewModel: ProcessViewModel = viewModel()
    val teacherDetailsViewModel: TeacherDetailsViewModel = viewModel()
    val context = LocalContext.current
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    
    // Initialize ViewModel with context
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Observe UI State
    val uiState: ProcessUiState by viewModel.uiState.collectAsState()
    
    // Handle UI State changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is ProcessUiState.Completed -> {
                // When processing is complete, initialize teacher details data
                teacherDetailsViewModel.initialize(context)
                // Wait for teacher details to be initialized
                delay(500)
                
                // Then show success message
                snackbarHostState.showSnackbar(
                    message = "Processing completed successfully",
                    duration = SnackbarDuration.Short
                )
            }
            is ProcessUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (uiState as ProcessUiState.Error).message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process Data Files") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title and explanation
            Text(
                text = "Process Uploaded Files",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "This step analyzes the timetable and substitute data files to prepare the app for operation.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Process information card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Information",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Why Process Data?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "• Validates the structure of your uploaded files",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• Extracts teacher information and class schedules",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• Prepares data for the substitute assignment algorithm",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• Creates necessary internal data structures",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "This step is required after uploading new files or making changes to existing files.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Current status box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = when (uiState) {
                    is ProcessUiState.Completed -> MaterialTheme.colorScheme.tertiaryContainer
                    is ProcessUiState.Error -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (uiState) {
                            is ProcessUiState.Completed -> Icons.Default.CheckCircle
                            is ProcessUiState.Error -> Icons.Default.Error
                            else -> Icons.Default.Info
                        },
                        contentDescription = "Status Icon",
                        tint = when (uiState) {
                            is ProcessUiState.Completed -> MaterialTheme.colorScheme.tertiary
                            is ProcessUiState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = when (uiState) {
                            is ProcessUiState.Ready -> "Ready to process files"
                            is ProcessUiState.Processing -> "Processing in progress..."
                            is ProcessUiState.Completed -> "Processing completed successfully"
                            is ProcessUiState.Error -> "Error: ${(uiState as ProcessUiState.Error).message}"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (uiState) {
                            is ProcessUiState.Completed -> MaterialTheme.colorScheme.onTertiaryContainer
                            is ProcessUiState.Error -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (uiState) {
                is ProcessUiState.Processing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Processing files...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "This may take a moment depending on file size",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    // Process button
                    Button(
                        onClick = { 
                            viewModel.processTimetable()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = uiState !is ProcessUiState.Processing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (uiState) {
                                is ProcessUiState.Completed -> MaterialTheme.colorScheme.tertiary
                                is ProcessUiState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Icon(
                            imageVector = when (uiState) {
                                is ProcessUiState.Completed -> Icons.Default.CheckCircle
                                is ProcessUiState.Error -> Icons.Default.Error
                                else -> Icons.Default.Refresh
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (uiState) {
                                is ProcessUiState.Ready -> "START PROCESSING"
                                is ProcessUiState.Processing -> "PROCESSING..."
                                is ProcessUiState.Completed -> "PROCESS AGAIN"
                                is ProcessUiState.Error -> "RETRY PROCESSING"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Additional information for completed state
                    if (uiState is ProcessUiState.Completed) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Processing Complete",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Your data has been successfully processed. You can now use all app features.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Remember to process again if you make changes to your data files.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 