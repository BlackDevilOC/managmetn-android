package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.substituemanagment.managment.ui.viewmodels.ProcessUiState
import com.substituemanagment.managment.ui.viewmodels.ProcessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessScreen() {
    val viewModel: ProcessViewModel = viewModel()
    val context = LocalContext.current
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    
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
                title = { Text("Process Data") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is ProcessUiState.Processing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Processing timetable data...")
                    }
                }
                else -> {
                    Button(
                        onClick = { viewModel.processTimetable() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = uiState !is ProcessUiState.Processing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (uiState) {
                                is ProcessUiState.Completed -> MaterialTheme.colorScheme.primary
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
                            when (uiState) {
                                is ProcessUiState.Ready -> "Start Processing"
                                is ProcessUiState.Processing -> "Processing..."
                                is ProcessUiState.Completed -> "Processing Complete"
                                is ProcessUiState.Error -> "Retry Processing"
                            }
                        )
                    }
                }
            }
        }
    }
} 