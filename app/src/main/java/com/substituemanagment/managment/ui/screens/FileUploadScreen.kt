package com.substituemanagment.managment.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.substituemanagment.managment.data.FileType
import com.substituemanagment.managment.ui.viewmodels.FileUploadViewModel
import com.substituemanagment.managment.ui.viewmodels.FileUploadUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileUploadScreen() {
    val viewModel: FileUploadViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Initialize ViewModel with context
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Observe UI State
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle UI State changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is FileUploadUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = (uiState as FileUploadUiState.Success).message,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetState()
            }
            is FileUploadUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (uiState as FileUploadUiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // File picker launchers
    val timetableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(it, FileType.TIMETABLE, context)
        }
    }

    val substituteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(it, FileType.SUBSTITUTE, context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Upload") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // First Upload Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Timetable File",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Upload the timetable CSV file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { timetableLauncher.launch("text/csv") },
                            modifier = Modifier.align(Alignment.End),
                            enabled = uiState !is FileUploadUiState.Loading
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = "Upload",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Timetable")
                        }
                    }
                }

                // Second Upload Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Substitute File",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Upload the substitute teachers CSV file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { substituteLauncher.launch("text/csv") },
                            modifier = Modifier.align(Alignment.End),
                            enabled = uiState !is FileUploadUiState.Loading
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = "Upload",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Substitute List")
                        }
                    }
                }

                if (uiState is FileUploadUiState.Loading) {
                    CircularProgressIndicator()
                }
            }

            // Process Button at the bottom
            Button(
                onClick = { /* TODO: Implement processing */ },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Process Timetables",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
} 