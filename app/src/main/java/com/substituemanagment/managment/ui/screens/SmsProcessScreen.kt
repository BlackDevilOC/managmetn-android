package com.substituemanagment.managment.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsProcessScreen(navController: NavController) {
    val viewModel: SmsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State for phone number editing
    val selectedTeachers = remember { mutableStateListOf<SmsViewModel.TeacherWithAssignments>() }
    val editingPhoneNumber = remember { mutableStateMapOf<String, Boolean>() }
    val phoneNumbers = remember { mutableStateMapOf<String, String>() }
    
    val isLoading by remember { viewModel.isLoading }
    val errorMessage by remember { viewModel.errorMessage }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccessBar by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // Check if there are any selected teachers and prepare them
    LaunchedEffect(Unit) {
        // Load selected teachers from the saved file
        viewModel.loadSelectedTeachersFromFile()
        
        // Add a delay to ensure the file loading completes
        delay(500)
        
        // Log the teachers to process
        Log.d("SmsProcessScreen", "Teachers to process count: ${viewModel.teachersToProcess.size}")
        
        // Clear and reload the teachers from the ViewModel
        selectedTeachers.clear()
        selectedTeachers.addAll(viewModel.teachersToProcess)
        
        Log.d("SmsProcessScreen", "Selected teachers count after reload: ${selectedTeachers.size}")
        
        if (selectedTeachers.isEmpty()) {
            // No teachers were selected, show error message then navigate back
            Log.d("SmsProcessScreen", "No teachers selected, showing error and navigating back")
            snackbarHostState.showSnackbar(
                message = "No teachers selected for SMS. Please select teachers first.",
                duration = SnackbarDuration.Short
            )
            // Navigate back after showing the message
            delay(1500)
            navController.navigate(Screen.SmsSend.route)
        } else {
            // Initialize phone numbers from viewModel data
            Log.d("SmsProcessScreen", "Initializing phone numbers for ${selectedTeachers.size} teachers")
            selectedTeachers.forEach { teacher ->
                phoneNumbers[teacher.name] = teacher.phone
                // Mark for editing if phone is empty or invalid
                editingPhoneNumber[teacher.name] = teacher.phone.isBlank() || !viewModel.isValidPhoneNumber(teacher.phone)
            }
        }
    }
    
    // Function to show success message
    fun showSuccess(message: String) {
        successMessage = message
        showSuccessBar = true
        // Auto-hide after a delay
        scope.launch {
            delay(3000)
            showSuccessBar = false
        }
    }
    
    // Show error message in snackbar if there is one
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Selected Teachers") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.SmsSend.route) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Small history icon to view SMS history
                    IconButton(
                        onClick = { 
                            // Navigate to SMS screen with history flag
                            navController.navigate(Screen.SmsSend.route)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "SMS History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Update phone numbers in viewModel
                    val invalidPhones = selectedTeachers.filter { teacher -> 
                        val phone = phoneNumbers[teacher.name] ?: ""
                        phone.isBlank() || !viewModel.isValidPhoneNumber(phone)
                    }
                    
                    if (invalidPhones.isEmpty()) {
                        // Update phone numbers in viewModel
                        selectedTeachers.forEach { teacher ->
                            val phone = phoneNumbers[teacher.name] ?: ""
                            viewModel.updatePhoneNumber(teacher.name, phone)
                        }
                        
                        // Send SMS with success callback
                        if (viewModel.checkSmsPermissions()) {
                            viewModel.sendSmsToTeachersOneByOne(context) {
                                showSuccess("SMS sent successfully")
                                scope.launch {
                                    delay(1500)
                                    navController.navigate(Screen.SmsSend.route)
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "SMS permission is required to send messages",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Please fix invalid phone numbers before proceeding",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Send, contentDescription = "Send SMS") },
                text = { Text("Send SMS") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Show loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Show the error message as a progress indicator
                        errorMessage?.let {
                            if (it.startsWith("Sending SMS")) {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            // Success bar with animation
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
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Selected Teachers for SMS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${selectedTeachers.size} teachers selected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Please verify phone numbers before sending SMS.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Display selected teachers with phone verification
                if (selectedTeachers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(bottom = 8.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "No teachers selected for SMS notification",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedTeachers) { teacher ->
                            TeacherPhoneVerificationItem(
                                teacherName = teacher.name,
                                phoneNumber = phoneNumbers[teacher.name] ?: "",
                                isEditing = editingPhoneNumber[teacher.name] ?: false,
                                onEditClick = { editingPhoneNumber[teacher.name] = true },
                                onPhoneChange = { phoneNumbers[teacher.name] = it },
                                onDone = { editingPhoneNumber[teacher.name] = false },
                                isValid = viewModel.isValidPhoneNumber(phoneNumbers[teacher.name] ?: ""),
                                assignmentsSummary = getSummaryForAssignments(teacher.assignments)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function to create a summary of assignments
private fun getSummaryForAssignments(assignments: List<SmsViewModel.AssignmentData>): String {
    if (assignments.isEmpty()) return "No assignments"
    
    val sortedAssignments = assignments.sortedBy { it.period }
    return if (sortedAssignments.size == 1) {
        val assignment = sortedAssignments.first()
        "Period ${assignment.period}: ${assignment.className} (for ${assignment.originalTeacher})"
    } else {
        "Multiple assignments (${sortedAssignments.size}): " + 
        sortedAssignments.joinToString(", ") { "P${it.period}" }
    }
}

@Composable
fun TeacherPhoneVerificationItem(
    teacherName: String,
    phoneNumber: String,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onDone: () -> Unit,
    isValid: Boolean,
    assignmentsSummary: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teacherName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (assignmentsSummary.isNotEmpty()) {
                        Text(
                            text = assignmentsSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (!isEditing) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Phone",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isEditing) {
                // Phone number editing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { onPhoneChange(it) },
                        label = { Text("Phone Number") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        isError = !isValid
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onDone,
                        enabled = isValid
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
                
                if (!isValid) {
                    Text(
                        text = "Please enter a valid phone number",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            } else {
                // Display phone number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (phoneNumber.isBlank()) "No phone number provided" else phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (phoneNumber.isBlank() || !isValid) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (phoneNumber.isBlank() || !isValid) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Invalid",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Validate phone number format (accepts numbers with optional dashes, spaces, or parentheses)
fun isValidPhoneNumber(phone: String): Boolean {
    if (phone.isBlank()) return false
    val phoneRegex = Regex("^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}\$")
    return phone.matches(phoneRegex)
} 