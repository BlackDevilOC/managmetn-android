package com.substituemanagment.managment.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
    val scrollState = rememberScrollState()
    
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
        // Check file existence on start
        hasTimetable = viewModel.checkFileExists(FileType.TIMETABLE)
        hasSubstitute = viewModel.checkFileExists(FileType.SUBSTITUTE)
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
                
                // Refresh file status after successful upload
                hasTimetable = viewModel.checkFileExists(FileType.TIMETABLE)
                hasSubstitute = viewModel.checkFileExists(FileType.SUBSTITUTE)
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
                        fontWeight = FontWeight.Bold
                    )
                    
                    Divider()
                    
                    Text(
                        text = "Please select your data files for upload. The app works with CSV or JSON files:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "• timetable.json or timetable.csv - Contains class schedule data",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "• substitutes.json or substitutes.csv - Contains substitute teacher list",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { timetableLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is FileUploadUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasTimetable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (hasTimetable) Icons.Default.CheckCircle else Icons.Default.Upload,
                            contentDescription = "Upload",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasTimetable) "Timetable Uploaded ✓" else "Upload Timetable")
                    }
                    
                    Button(
                        onClick = { substituteLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is FileUploadUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasSubstitute) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (hasSubstitute) Icons.Default.CheckCircle else Icons.Default.Upload,
                            contentDescription = "Upload",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasSubstitute) "Substitute List Uploaded ✓" else "Upload Substitute List")
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
                title = { Text("File Upload & Processing") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Instructions header
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
                            Text(
                                text = "Upload & Process Instructions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = buildAnnotatedString {
                                    append("1. ")
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Upload both required files ")
                                    }
                                    append("(timetable and substitute list)")
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = buildAnnotatedString {
                                    append("2. ")
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Verify ")
                                    }
                                    append("that both files are present")
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = buildAnnotatedString {
                                    append("3. ")
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)) {
                                        append("Click Process ")
                                    }
                                    append("to analyze the files and prepare the app")
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "You must complete all steps for the app to function properly!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // File status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "File Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasTimetable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = if (hasTimetable) "Present" else "Missing",
                                    tint = if (hasTimetable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "Timetable: " + if (hasTimetable) "Present" else "Missing",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasSubstitute) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = if (hasSubstitute) "Present" else "Missing",
                                    tint = if (hasSubstitute) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "Substitute List: " + if (hasSubstitute) "Present" else "Missing",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

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
                            containerColor = MaterialTheme.colorScheme.secondary
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
                    
                    // Processing step information
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
                                text = "Processing Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "• The Process button will analyze your uploaded files",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "• This step is required after uploading new files",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "• Processing may take a few moments depending on file size",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "• The app cannot function properly without processing",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Process Button at the bottom with emphasis
            if (!isVerifying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            // Redirect to Process screen
                            navController.navigate(Screen.Process.route)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = hasTimetable && hasSubstitute,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Process Files",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROCESS FILES",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
} 