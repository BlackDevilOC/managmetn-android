package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import java.io.File

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process SMS") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        try {
                            // Prepare teachers for SMS
                            val teachersWithPhone = sortedTeachers.filter { teacher ->
                                val phone = phoneNumbers[teacher]
                                !phone.isNullOrEmpty()
                            }

                            if (teachersWithPhone.isEmpty()) {
                                snackbarHostState.showSnackbar("No valid phone numbers found")
                                return@launch
                            }

                            // Send SMS to each teacher
                            var successCount = 0
                            var failedCount = 0
                            val failedTeachers = mutableListOf<String>()

                            for (teacher in teachersWithPhone) {
                                val phone = phoneNumbers[teacher] ?: continue
                                val message = "Dear $teacher, you have been assigned as a substitute teacher. Please confirm your availability."

                                try {
                                    viewModel.sendSmsToTeachersOneByOne(context) {
                                        // Save to history
                                        viewModel.addToHistory(
                                            teacherName = teacher,
                                            phoneNumber = phone,
                                            message = message,
                                            status = "Sent"
                                        )
                                        successCount++
                                    }
                                } catch (e: Exception) {
                                    failedCount++
                                    failedTeachers.add(teacher)
                                    // Save failed attempt to history
                                    viewModel.addToHistory(
                                        teacherName = teacher,
                                        phoneNumber = phone,
                                        message = message,
                                        status = "Failed"
                                    )
                                }
                            }

                            // Show summary
                            val summary = when {
                                successCount == teachersWithPhone.size -> "All SMS sent successfully"
                                successCount > 0 -> "$successCount SMS sent, $failedCount failed"
                                else -> "Failed to send any SMS"
                            }
                            snackbarHostState.showSnackbar(summary)

                            // Navigate to history if any SMS were sent
                            if (successCount > 0) {
                                navController.navigate("sms_history")
                            }
                        } catch (e: Exception) {
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
                        contentDescription = "Send SMS"
                    )
                    Text("Send SMS")
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