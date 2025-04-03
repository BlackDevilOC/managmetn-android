package com.substituemanagment.managment.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.substituemanagment.managment.data.FileType
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.ui.viewmodels.FileUploadViewModel
import com.substituemanagment.managment.ui.viewmodels.FileUploadUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileUploadScreen(navController: NavController) {
    val viewModel: FileUploadViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State for dialog visibility
    var showUploadDialog by remember { mutableStateOf(false) }
    
    // State for verification loading
    var isVerifying by remember { mutableStateOf(false) }
    
    // State for file presence
    var hasTimetable by remember { mutableStateOf(false) }
    var hasSubstitute by remember { mutableStateOf(false) }
    
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
                // Don't automatically close dialog
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

    // Upload Dialog
    if (showUploadDialog) {
        Dialog(onDismissRequest = { showUploadDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select File to Upload",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Button(
                        onClick = { timetableLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
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
                    
                    Button(
                        onClick = { substituteLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
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

                    if (uiState is FileUploadUiState.Loading) {
                        CircularProgressIndicator()
                    }

                    // Close dialog button
                    Button(
                        onClick = { showUploadDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
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
            if (isVerifying) {
                // Loading screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Verifying Files...")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Main upload button
                    Button(
                        onClick = { showUploadDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = "Upload Files",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Files")
                    }

                    // Verify/Submit button
                    Button(
                        onClick = {
                            scope.launch {
                                isVerifying = true
                                // Add small delay to show loading state
                                delay(1000)
                                hasTimetable = viewModel.checkFileExists(FileType.TIMETABLE)
                                hasSubstitute = viewModel.checkFileExists(FileType.SUBSTITUTE)
                                isVerifying = false
                                
                                val message = when {
                                    hasTimetable && hasSubstitute -> "Both files are present and ready for processing"
                                    !hasTimetable && !hasSubstitute -> "Please upload both Timetable and Substitute files"
                                    !hasTimetable -> "Please upload Timetable file"
                                    !hasSubstitute -> "Please upload Substitute file"
                                    else -> "Unknown error occurred"
                                }
                                
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verify Files",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify Files")
                    }
                }
            }

            // Process Button at the bottom
            if (!isVerifying) {
                Button(
                    onClick = { navController.navigate(Screen.Process.route) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = hasTimetable && hasSubstitute,
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
} 