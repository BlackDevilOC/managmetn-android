package com.substituemanagment.managment.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.substituemanagment.managment.BuildConfig
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController = rememberNavController()) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(context)
    )
    val homeState = homeViewModel.state
    
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var showWelcomeAnimation by remember { mutableStateOf(true) }
    
    // Animation states
    val scrollState = rememberScrollState()
    val scale = remember { Animatable(0.8f) }
    
    // Animation effect on launch
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(1000)
        showWelcomeAnimation = false
    }
    
    val refreshFunction = {
        scope.launch {
            refreshing = true
            homeViewModel.refreshData()
            delay(1500) // Animation time
            refreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(
                            visible = !refreshing,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Text(
                                "Teacher Substitution",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = refreshing,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Text(
                                "Refreshing...",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        var showTimePickerDialog by remember { mutableStateOf(false) }
                        
                        Text(
                            text = "Auto Reset",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.clickable { showTimePickerDialog = true }
                        )
                        Switch(
                            checked = homeState.autoResetEnabled,
                            onCheckedChange = { homeViewModel.toggleAutoReset() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        if (showTimePickerDialog) {
                            var selectedHour by remember { mutableStateOf(homeState.autoResetHour) }
                            var selectedMinute by remember { mutableStateOf(homeState.autoResetMinute) }
                            
                            AlertDialog(
                                onDismissRequest = { showTimePickerDialog = false },
                                title = {
                                    Text(
                                        "Auto Reset Configuration",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            "Select the time when the app should automatically reset attendance and substitutions.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Hour Picker
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Hour", style = MaterialTheme.typography.labelSmall)
                                                Surface(
                                                    modifier = Modifier
                                                        .width(80.dp)
                                                        .padding(4.dp),
                                                    shape = MaterialTheme.shapes.small,
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                selectedHour = if (selectedHour <= 0) 23 else selectedHour - 1
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Decrease hour",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        
                                                        Text(
                                                            text = selectedHour.toString().padStart(2, '0'),
                                                            style = MaterialTheme.typography.titleLarge,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                        
                                                        IconButton(
                                                            onClick = {
                                                                selectedHour = if (selectedHour >= 23) 0 else selectedHour + 1
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                                contentDescription = "Increase hour",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                // AM/PM indicator
                                                Text(
                                                    text = if (selectedHour < 12) "AM" else "PM",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            Text(":", style = MaterialTheme.typography.titleLarge)
                                            
                                            // Minute Picker
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Minute", style = MaterialTheme.typography.labelSmall)
                                                Surface(
                                                    modifier = Modifier
                                                        .width(80.dp)
                                                        .padding(4.dp),
                                                    shape = MaterialTheme.shapes.small,
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                selectedMinute = if (selectedMinute <= 0) 59 else selectedMinute - 1
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Decrease minute",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        
                                                        Text(
                                                            text = selectedMinute.toString().padStart(2, '0'),
                                                            style = MaterialTheme.typography.titleLarge,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                        
                                                        IconButton(
                                                            onClick = {
                                                                selectedMinute = if (selectedMinute >= 59) 0 else selectedMinute + 1
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                                contentDescription = "Increase minute",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            "Current setting: ${homeState.autoResetHour.toString().padStart(2, '0')}:${homeState.autoResetMinute.toString().padStart(2, '0')} ${if (homeState.autoResetHour < 12) "AM" else "PM"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            homeViewModel.setResetTime(
                                                hour = selectedHour,
                                                minute = selectedMinute,
                                                isAM = selectedHour < 12
                                            )
                                            showTimePickerDialog = false
                                        }
                                    ) {
                                        Text("Save")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showTimePickerDialog = false }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quick Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { refreshFunction() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (refreshing) Icons.Default.Refresh else Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }


            
            // Welcome animation on first load
            AnimatedVisibility(
                visible = showWelcomeAnimation,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
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
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome to Teacher Substitution",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                    }
                }
            }
            
            // Current Period Widget (New)
            AnimatedVisibility(
                visible = homeState.currentPeriod != null || homeState.nextPeriod != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Current Period",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Class Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (homeState.currentPeriod != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(40.dp),
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${homeState.currentPeriod}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column {
                                    Text(
                                        text = "Current Period",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = homeState.currentPeriodTimeRange,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ) {
                                    Text(
                                        text = "NOW",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                            )
                        }
                        
                        if (homeState.nextPeriod != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp),
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${homeState.nextPeriod}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column {
                                    Text(
                                        text = if (homeState.currentPeriod == null) "Next Period" else "Up Next",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = homeState.nextPeriodTimeRange,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        
                        if (homeState.currentPeriod == null && homeState.nextPeriod == null) {
                            Text(
                                text = "No active periods at this time",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        TextButton(
                            onClick = { navController.navigate(Screen.Schedule.route) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("View Schedule")
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "View Schedule",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Today's Statistics Card with Animation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Today's Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Statistics Row - Now using dynamic data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(
                            icon = Icons.Default.Person,
                            title = "Absent",
                            value = "${homeState.absentTeachersCount}",
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        StatCard(
                            icon = Icons.Default.SwapHoriz,
                            title = "Substitutions",
                            value = "${homeState.substitutionsCount}",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        
                        StatCard(
                            icon = Icons.Default.School,
                            title = "Classes",
                            value = "${homeState.classesCount}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Dynamic message based on absent teachers
                    Text(
                        text = if (homeState.absentTeachersCount > 0) 
                            "${homeState.absentTeachersCount} teacher(s) reported absent today" 
                            else "No absences reported for today",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Quick Actions with Animation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .align(Alignment.Start)
                )
                
                // Quick Actions Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        text = "Report Absence",
                        onClick = { /* TODO: Implement report absence */ },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    QuickActionButton(
                        icon = Icons.Default.CalendarViewDay,
                        text = "View Schedule",
                        onClick = { navController.navigate(Screen.Schedule.route) },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.ManageAccounts,
                        text = "Manage Teachers",
                        onClick = { navController.navigate(Screen.Teachers.route) },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    QuickActionButton(
                        icon = Icons.Default.CloudUpload,
                        text = "File Upload",
                        onClick = { navController.navigate(Screen.FileUpload.route) },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            // Big Feature Buttons - Removing Algorithm Testing button
            FeatureButton(
                title = "View Substitutions",
                description = "Check assigned substitutions for all teachers",
                icon = Icons.Default.List,
                onClick = { navController.navigate(Screen.ViewSubstitutions.route) },
                color = MaterialTheme.colorScheme.secondaryContainer
            )
            
            FeatureButton(
                title = "SMS Notifications",
                description = "Send SMS notifications to teachers",
                icon = Icons.Default.Send,
                onClick = { navController.navigate(Screen.SmsSend.route) },
                color = MaterialTheme.colorScheme.tertiaryContainer
            )

            // Current Teacher Schedule - Replaces Recent Substitutions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = "Teacher Schedule",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Current Teacher Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (homeState.currentPeriod == null) {
                        Text(
                            text = "No active period at this time",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else if (homeState.currentSchedules.isEmpty()) {
                        Text(
                            text = "No scheduled classes for this period",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        // Display information about current period and time
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Period ${homeState.currentPeriod}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = homeState.currentPeriodTimeRange,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // List of current teachers and their classes
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            homeState.currentSchedules.forEachIndexed { index, schedule ->
                                TeacherClassRow(
                                    teacherName = schedule.teacherName,
                                    className = schedule.className,
                                    isAlternateRow = index % 2 == 1
                                )
                                
                                if (index < homeState.currentSchedules.size - 1) {
                                    Divider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = { navController.navigate(Screen.Schedule.route) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("View Full Schedule")
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "View Schedule",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        shadowElevation = 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FeatureButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color
) {
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(buttonScale)
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Reset pressed state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
fun TeacherClassRow(
    teacherName: String,
    className: String,
    isAlternateRow: Boolean = false
) {
    Surface(
        color = if (isAlternateRow) 
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        else
            MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Teacher avatar and name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Teacher avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = teacherName.first().toString().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = teacherName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Class badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = className,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
} 