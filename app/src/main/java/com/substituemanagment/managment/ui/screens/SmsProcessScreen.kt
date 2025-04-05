package com.substituemanagment.managment.ui.screens

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsProcessScreen(navController: NavController) {
    val viewModel: SmsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State for phone number editing
    val selectedTeachers = remember { viewModel.selectedTeachersForSms }
    val editingPhoneNumber = remember { mutableStateMapOf<String, Boolean>() }
    val phoneNumbers = remember { mutableStateMapOf<String, String>() }
    
    // Initialize phone numbers from viewModel data
    LaunchedEffect(Unit) {
        viewModel.selectedTeachersForSms.forEach { teacher ->
            phoneNumbers[teacher.id] = teacher.phone
            // Mark for editing if phone is empty or invalid
            editingPhoneNumber[teacher.id] = teacher.phone.isBlank() || !isValidPhoneNumber(teacher.phone)
        }
    }
    
    val isLoading by remember { viewModel.isLoading }
    val errorMessage by remember { viewModel.errorMessage }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error message in snackbar if there is one
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Phone Numbers") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Update phone numbers in viewModel
                    val invalidPhones = selectedTeachers.filter { 
                        phoneNumbers[it.id]?.isBlank() == true || !isValidPhoneNumber(phoneNumbers[it.id] ?: "")
                    }
                    
                    if (invalidPhones.isEmpty()) {
                        // Update phone numbers in viewModel
                        viewModel.updatePhoneNumbers(phoneNumbers)
                        // Navigate to SMS send screen
                        navController.navigate(Screen.SmsSend.route)
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
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Send, contentDescription = "Continue to SMS")
            }
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Verify Recipient Phone Numbers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Please verify the phone numbers for each teacher before sending SMS notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Display selected teachers with phone verification
                if (selectedTeachers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No teachers selected for SMS notification",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedTeachers) { teacher ->
                            TeacherPhoneVerificationItem(
                                teacherName = teacher.name,
                                phoneNumber = phoneNumbers[teacher.id] ?: "",
                                isEditing = editingPhoneNumber[teacher.id] ?: false,
                                onEditClick = { editingPhoneNumber[teacher.id] = true },
                                onPhoneChange = { phoneNumbers[teacher.id] = it },
                                onDone = { editingPhoneNumber[teacher.id] = false },
                                isValid = isValidPhoneNumber(phoneNumbers[teacher.id] ?: "")
                            )
                        }
                    }
                }
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
    onDone: () -> Unit,
    isValid: Boolean
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
                Text(
                    text = teacherName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
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