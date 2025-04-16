package com.substituemanagment.managment.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import com.substituemanagment.managment.ui.viewmodels.TeacherDetailsViewModel
import kotlinx.coroutines.launch

@Composable
fun SmsProcessScreen(navController: NavController) {
    val viewModel: SmsViewModel = viewModel()
    val teacherDetailsViewModel: TeacherDetailsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State for UI
    val selectedTeachers = remember { mutableStateListOf<String>() }
    val editingPhoneNumber = remember { mutableStateMapOf<String, String>() }
    val originalPhoneStatus = remember { mutableStateMapOf<String, Boolean>() }
    
    // State for success message
    val showSuccessBar = remember { mutableStateOf(false) }
    val successMessage = remember { mutableStateOf("") }
    
    // State for auto-loaded info
    val showAutoLoadedInfo = remember { mutableStateOf(false) }
    val autoLoadedCount = remember { mutableStateOf(0) }
    
    // State for saving phone number dialog
    val showSaveNumberDialog = remember { mutableStateOf(false) }
    val currentTeacherForSaving = remember { mutableStateOf<String?>(null) }
    val currentNumberForSaving = remember { mutableStateOf("") }
    
    // Error state
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    
    // Prepare data for SMS
    LaunchedEffect(Unit) {
        viewModel.prepareSelectedTeachersForSms()
        
        // Initialize editing state for phone numbers
        viewModel.teachersToProcess.forEach { teacher ->
            val phone = teacher.phone
            editingPhoneNumber[teacher.name] = phone
            originalPhoneStatus[teacher.name] = viewModel.isValidPhoneNumber(phone)
        }
        
        // Count auto-loaded phone numbers
        val autoLoaded = viewModel.phoneNumberSources.count { it.value == "Auto-loaded" }
        autoLoadedCount.value = autoLoaded
        showAutoLoadedInfo.value = autoLoaded > 0
    }
    
    // Show error message if any
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Phone Numbers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Auto-loaded info
                    if (showAutoLoadedInfo.value) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Auto-loaded ${autoLoadedCount.value} phone numbers from system. Please verify them before sending SMS.",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { showAutoLoadedInfo.value = false }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // Phone number verification
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                "Verify Phone Numbers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(viewModel.teachersToProcess) { teacher ->
                                    val teacherName = teacher.name
                                    val phone = editingPhoneNumber[teacherName] ?: ""
                                    val isEditing = selectedTeachers.contains(teacherName)
                                    val isValid = viewModel.isValidPhoneNumber(phone)
                                    val hadValidPhoneInitially = originalPhoneStatus[teacherName] ?: false
                                    val assignments = teacher.assignments
                                    val assignmentsSummary = getSummaryForAssignments(assignments)
                                    val isAutoLoaded = viewModel.phoneNumberSources[teacherName] == "Auto-loaded"
                                    
                                    TeacherPhoneVerificationItem(
                                        teacherName = teacherName,
                                        phoneNumber = phone,
                                        isEditing = isEditing,
                                        onEditClick = { 
                                            if (isEditing) {
                                                selectedTeachers.remove(teacherName)
                                            } else {
                                                selectedTeachers.add(teacherName)
                                            }
                                        },
                                        onPhoneChange = { newPhone ->
                                            editingPhoneNumber[teacherName] = newPhone
                                        },
                                        onDone = { newPhone ->
                                            viewModel.updatePhoneNumber(teacherName, newPhone)
                                            selectedTeachers.remove(teacherName)
                                            
                                            // Ask to save the phone number if it's valid and different
                                            if (viewModel.isValidPhoneNumber(newPhone) && 
                                                !hadValidPhoneInitially) {
                                                currentTeacherForSaving.value = teacherName
                                                currentNumberForSaving.value = newPhone
                                                showSaveNumberDialog.value = true
                                            }
                                        },
                                        isValid = isValid,
                                        assignmentsSummary = assignmentsSummary,
                                        isAutoLoaded = isAutoLoaded,
                                        hadValidPhoneInitially = hadValidPhoneInitially
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { navController.popBackStack() }
                                ) {
                                    Text("Back")
                                }
                                
                                Button(
                                    onClick = {
                                        // Update all phone numbers
                                        editingPhoneNumber.forEach { (teacherName, phone) ->
                                            viewModel.updatePhoneNumber(teacherName, phone)
                                        }
                                        
                                        // Check if all phone numbers are valid
                                        if (viewModel.checkAllPhoneNumbersValid()) {
                                            navController.navigate(Screen.SmsSend.route)
                                        } else {
                                            val missingPhones = viewModel.getTeachersWithMissingPhoneNumbers()
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Please fix invalid phone numbers for: ${missingPhones.joinToString(", ")}"
                                                )
                                            }
                                        }
                                    },
                                    enabled = viewModel.teachersToProcess.isNotEmpty()
                                ) {
                                    Text("Continue to Send SMS")
                                }
                            }
                        }
                    }
                }
            }
            
            // Success message
            if (showSuccessBar.value) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            successMessage.value,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Save phone number dialog
            if (showSaveNumberDialog.value) {
                AlertDialog(
                    onDismissRequest = { showSaveNumberDialog.value = false },
                    title = { Text("Save Phone Number") },
                    text = { 
                        Text("Would you like to save this phone number for ${currentTeacherForSaving.value} in the teacher database?") 
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // In a real app, this would save to the teacher database
                                currentTeacherForSaving.value?.let { teacher ->
                                    teacherDetailsViewModel.updateTeacherPhone(
                                        teacher, 
                                        currentNumberForSaving.value
                                    )
                                    successMessage.value = "Phone number saved for $teacher"
                                    showSuccessBar.value = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(3000)
                                        showSuccessBar.value = false
                                    }
                                }
                                showSaveNumberDialog.value = false
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showSaveNumberDialog.value = false }
                        ) {
                            Text("Not Now")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TeacherPhoneVerificationItem(
    teacherName: String,
    phoneNumber: String,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onDone: (String) -> Unit,
    isValid: Boolean,
    assignmentsSummary: String = "",
    isAutoLoaded: Boolean = false,
    hadValidPhoneInitially: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        teacherName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (!isEditing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isValid) Color.Unspecified else Color.Red
                            )
                            
                            if (isAutoLoaded) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "Auto",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            if (!isValid) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Invalid",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { onPhoneChange(it) },
                            label = { Text("Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = !isValid,
                            supportingText = {
                                if (!isValid) {
                                    Text("Invalid phone number")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onEditClick() }
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = { onDone(phoneNumber) },
                                enabled = isValid
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
                
                if (!isEditing) {
                    IconButton(
                        onClick = { onEditClick() }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                }
            }
            
            if (assignmentsSummary.isNotEmpty() && !isEditing) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    assignmentsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getSummaryForAssignments(assignments: List<SmsViewModel.AssignmentData>): String {
    if (assignments.isEmpty()) return ""
    
    return if (assignments.size == 1) {
        val assignment = assignments[0]
        "Assignment: ${assignment.className} (Period ${assignment.period})"
    } else {
        "Assignments: ${assignments.size} classes"
    }
}

fun isValidPhoneNumber(phone: String): Boolean {
    // Simple validation - should be at least 10 digits
    return phone.replace(Regex("[^0-9]"), "").length >= 10
}
