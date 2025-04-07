package com.substituemanagment.managment.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.ui.viewmodels.TeacherDetailsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDetailsScreen(navController: NavController) {
    val viewModel: TeacherDetailsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Editing states
    val editingTeacherId = remember { mutableStateOf<String?>(null) }
    val phoneNumberInput = remember { mutableStateOf("") }
    
    // State for search query
    val searchQuery = remember { mutableStateOf("") }
    
    // Filter teachers based on search query
    val filteredTeachers = remember(viewModel.teachers, searchQuery.value) {
        if (searchQuery.value.isBlank()) {
            viewModel.teachers
        } else {
            viewModel.teachers.filter { 
                it.name.contains(searchQuery.value, ignoreCase = true) 
            }
        }
    }
    
    // Initialize on first launch
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Observe UI state changes
    LaunchedEffect(viewModel.uiState) {
        viewModel.uiState.collect { state ->
            when (state) {
                is TeacherDetailsViewModel.TeacherDetailsUiState.Success -> {
                    snackbarHostState.showSnackbar(
                        message = state.message,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.resetState()
                }
                is TeacherDetailsViewModel.TeacherDetailsUiState.Error -> {
                    snackbarHostState.showSnackbar(
                        message = state.message,
                        duration = SnackbarDuration.Long
                    )
                    viewModel.resetState()
                }
                else -> {}
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Details") },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Action Buttons
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Teacher Data Import Steps",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = "Step 1: Load teacher names from total_teachers.json file",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Button(
                            onClick = { viewModel.loadTeacherNamesFromTotalTeachers() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1. Load Teacher Names")
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(
                            text = "Step 2: Match phone numbers from Substitude_file.csv",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                        
                        Button(
                            onClick = { viewModel.matchPhoneNumbersFromSubstituteFile() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("2. Match Phone Numbers")
                        }
                    }
                }
                
                // Legend for colors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Missing phone number",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Has phone number",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Search field
                OutlinedTextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Search Teachers") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.value.isNotEmpty()) {
                            IconButton(onClick = { searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
                
                // Count summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredTeachers.size} teachers" +
                               if (searchQuery.value.isNotEmpty()) " for '${searchQuery.value}'" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "${viewModel.teachers.count { it.phoneNumber.isBlank() }} missing numbers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (viewModel.teachers.any { it.phoneNumber.isBlank() }) 
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Teachers list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredTeachers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonOff, 
                                        contentDescription = "No Teachers Found",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "No teachers found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Follow steps above to import teacher data:\n" +
                                               "1. Load teacher names from total_teachers.json\n" +
                                               "2. Match phone numbers from Substitude_file.csv",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredTeachers) { teacher ->
                            TeacherItem(
                                teacher = teacher,
                                isEditing = editingTeacherId.value == teacher.name,
                                onEditClick = {
                                    // Start editing this teacher
                                    editingTeacherId.value = teacher.name
                                    phoneNumberInput.value = teacher.phoneNumber
                                },
                                onSaveClick = {
                                    // Save phone number
                                    viewModel.updateTeacherPhone(teacher.name, phoneNumberInput.value)
                                    editingTeacherId.value = null
                                },
                                onCancelEdit = {
                                    // Cancel editing
                                    editingTeacherId.value = null
                                },
                                phoneNumber = phoneNumberInput,
                                onPhoneNumberChange = { phoneNumberInput.value = it }
                            )
                        }
                    }
                }
            }
            
            // Loading indicator
            if (viewModel.isLoading.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading teacher data...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherItem(
    teacher: TeacherDetailsViewModel.TeacherDetails,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelEdit: () -> Unit,
    phoneNumber: MutableState<String>,
    onPhoneNumberChange: (String) -> Unit
) {
    val hasPhoneNumber = teacher.phoneNumber.isNotBlank()
    val cardColor = if (hasPhoneNumber) 
        MaterialTheme.colorScheme.surface
    else 
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Teacher name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hasPhoneNumber) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Missing Phone Number",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                Text(
                    text = teacher.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Phone number section
            if (isEditing) {
                // Editing mode
                OutlinedTextField(
                    value = phoneNumber.value,
                    onValueChange = onPhoneNumberChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Save/Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancelEdit) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = onSaveClick) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            } else {
                // Display mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = if (hasPhoneNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = if (hasPhoneNumber) teacher.phoneNumber else "No phone number",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasPhoneNumber) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    // Edit button
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
} 