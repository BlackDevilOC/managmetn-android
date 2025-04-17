package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import com.substituemanagment.managment.utils.SmsSender
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

data class TeacherDetail(
    val isPresent: Boolean,
    val name: String,
    val phoneNumber: String,
    val variations: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsProcessScreen(
    navController: NavController,
    selectedTeachers: String?
) {
    val viewModel: SmsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val teachersList = selectedTeachers?.split(",") ?: emptyList()
    val phoneNumbers = remember { mutableStateMapOf<String, String>() }
    val sortedTeachers = remember { mutableStateListOf<String>() }
    
    // State for dialogs
    var showProgress by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0) }
    var totalTeachers by remember { mutableStateOf(0) }
    var isSending by remember { mutableStateOf(false) }

    // Function to get teacher phone number
    fun getTeacherPhoneNumber(teacherName: String): String? {
        try {
            val file = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data/teacher_details.json")
            if (file.exists()) {
                val jsonString = file.readText()
                val type = object : TypeToken<List<TeacherDetail>>() {}.type
                val details = Gson().fromJson<List<TeacherDetail>>(jsonString, type)
                
                // Search for teacher in details
                val teacherDetail = details.find { detail ->
                    detail.name.equals(teacherName, ignoreCase = true) || 
                    detail.variations.any { it.equals(teacherName, ignoreCase = true) }
                }
                
                return teacherDetail?.phoneNumber
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Load phone numbers for all teachers
    LaunchedEffect(Unit) {
        // Sort teachers: those without phone numbers first
        sortedTeachers.addAll(teachersList.sortedBy { teacher ->
            val phone = getTeacherPhoneNumber(teacher)
            if (phone.isNullOrEmpty()) 0 else 1
        })
        
        // Initialize phone numbers
        teachersList.forEach { teacher ->
            val phone = getTeacherPhoneNumber(teacher)
            if (!phone.isNullOrEmpty()) {
                phoneNumbers[teacher] = phone
            }
        }
    }

    // Function to save SMS history
    fun saveSmsHistory(teacher: String, phone: String, message: String, status: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        
        // Add to viewModel's history
        viewModel.smsHistory.add(0, SmsViewModel.SmsMessage(
            id = UUID.randomUUID().toString(),
            teacherName = teacher,
            teacherPhone = phone,
            message = message,
            timestamp = System.currentTimeMillis(),
            status = status
        ))
    }

    // Progress Dialog
    if (showProgress) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sending SMS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    CircularProgressIndicator(
                        progress = currentProgress.toFloat() / totalTeachers,
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp
                    )

                    Text(
                        text = "$currentProgress of $totalTeachers messages sent",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Button(
                        onClick = { 
                            showProgress = false
                            isSending = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Completion Dialog
    if (showCompletionDialog) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "SMS Sent Successfully",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "All messages have been sent successfully. Where would you like to go?",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                showCompletionDialog = false
                                navController.navigate("home")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Home")
                        }
                        Button(
                            onClick = { 
                                showCompletionDialog = false
                                navController.navigate("sms_history")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SMS History")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process SMS") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("sms_history") }) {
                        Icon(Icons.Default.History, contentDescription = "SMS History")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isSending) return@FloatingActionButton
                    
                    scope.launch {
                        try {
                            isSending = true
                            
                            // Get teachers with valid phone numbers
                            val teachersWithPhone = sortedTeachers.filter { teacher ->
                                val phone = phoneNumbers[teacher]
                                !phone.isNullOrEmpty()
                            }

                            if (teachersWithPhone.isEmpty()) {
                                snackbarHostState.showSnackbar("No valid phone numbers found")
                                isSending = false
                                return@launch
                            }

                            // Initialize progress tracking
                            currentProgress = 0
                            totalTeachers = teachersWithPhone.size
                            showProgress = true

                            // Send SMS to each teacher one by one
                            for (teacher in teachersWithPhone) {
                                if (!showProgress) break  // Stop if cancelled
                                
                                val phone = phoneNumbers[teacher] ?: continue
                                
                                // Get teacher's assignments from viewModel
                                val assignments = viewModel.teacherAssignments[teacher] ?: emptyList()
                                if (assignments.isEmpty()) continue
                                
                                // Sort assignments by period
                                val sortedAssignments = assignments.sortedBy { it.period }
                                
                                // Create detailed message
                                val dateStr = sortedAssignments.first().date
                                val dayStr = java.time.LocalDate.parse(dateStr).dayOfWeek.toString().lowercase()
                                    .replaceFirstChar { it.uppercase() }
                                
                                val message = if (sortedAssignments.size == 1) {
                                    val assignment = sortedAssignments.first()
                                    """
                                    Dear ${teacher.split(" ").firstOrNull() ?: teacher},
                                    You have been assigned as a substitute teacher for ${assignment.className} 
                                    Period: ${assignment.period}
                                    Date: $dateStr ($dayStr)
                                    Original Teacher: ${assignment.originalTeacher}
                                    Please confirm your availability.
                                    """.trimIndent()
                                } else {
                                    """
                                    Dear ${teacher.split(" ").firstOrNull() ?: teacher},
                                    You have been assigned to the following classes on $dateStr ($dayStr):
                                    
                                    ${sortedAssignments.joinToString("\n") { assignment ->
                                        "• Period ${assignment.period}: ${assignment.className} (for ${assignment.originalTeacher})"
                                    }}
                                    
                                    Please confirm your availability.
                                    """.trimIndent()
                                }
                                
                                // Send SMS and update progress
                                val success = SmsSender.sendSingleSms(
                                    context = context,
                                    phoneNumber = phone,
                                    message = message
                                )

                                // Save to history
                                saveSmsHistory(
                                    teacher = teacher,
                                    phone = phone,
                                    message = message,
                                    status = if (success) "Sent" else "Failed"
                                )

                                currentProgress++
                                delay(500) // Small delay to prevent overwhelming the system
                            }

                            if (showProgress) {  // Only show completion if not cancelled
                                showProgress = false
                                showCompletionDialog = true
                            }
                            isSending = false
                        } catch (e: Exception) {
                            showProgress = false
                            isSending = false
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send SMS",
                        tint = if (isSending) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Send SMS",
                        color = if (isSending) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (teachersList.isEmpty()) {
                Text(
                    text = "No teachers selected. Please go back and select teachers.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Selected Teachers (${teachersList.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedTeachers) { teacher ->
                        val hasPhoneNumber = !phoneNumbers[teacher].isNullOrEmpty()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Teacher",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = teacher,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedTextField(
                                    value = phoneNumbers[teacher] ?: "",
                                    onValueChange = { newValue ->
                                        phoneNumbers[teacher] = newValue
                                    },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("Enter phone number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (hasPhoneNumber) {
                                            Color.Green
                                        } else {
                                            Color.Red
                                        },
                                        unfocusedBorderColor = if (hasPhoneNumber) {
                                            Color.Green
                                        } else {
                                            Color.Red
                                        }
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Phone",
                                            tint = if (hasPhoneNumber) {
                                                Color.Green
                                            } else {
                                                Color.Red
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 