package com.substituemanagment.managment.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.ui.viewmodels.HomeViewModel
import com.substituemanagment.managment.ui.viewmodels.ClassInfo
import com.substituemanagment.managment.data.TeacherDataManager
import com.substituemanagment.managment.data.PeriodSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
            animationSpec = tween(
                durationMillis = 500,
                easing = EaseOutBack
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
                    IconButton(onClick = { refreshFunction() }) {
                        Icon(
                            imageVector = if (refreshing) Icons.Default.Refresh else Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.97f),
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
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

            // Current Classes Section - Enhanced with teacher info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Improved header layout with better spacing and alignment
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Current Classes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Current Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Get current date/time
                        val currentTime = remember {
                            mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")))
                        }
                        
                        // Keep the time updated
                        LaunchedEffect(Unit) {
                            while(true) {
                                currentTime.value = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
                                delay(30000) // Update every 30 seconds
                            }
                        }
                        
                        // Improved date/time display with better alignment
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = homeState.currentDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentTime.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    
                    if (homeState.currentPeriod != null) {
                        // Current Period Card with Teacher Info
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${homeState.currentPeriod}",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Period ${homeState.currentPeriod}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            // If class has a substitute teacher, show a badge
                                            homeState.currentClassInfo?.let { classInfo ->
                                                if (classInfo.isSubstituted) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                                    ) {
                                                        Text(
                                                            text = "SUB",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            text = homeState.currentPeriodTimeRange,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    // NOW badge
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                    }
                                }
                                
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                                )
                                
                                // Display current class information from HomeViewModel
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    homeState.currentClassInfo?.let { classInfo ->
                                        // Subject and Grade Information
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.School,
                                                    contentDescription = "Subject",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = classInfo.subject,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = classInfo.grade,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Teacher Information with substitution status
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = 
                                                    if (classInfo.isSubstituted) 
                                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                                    else
                                                        MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = "Teacher",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = if (classInfo.isSubstituted) 
                                                        MaterialTheme.colorScheme.tertiary 
                                                    else 
                                                        MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                if (classInfo.isSubstituted) {
                                                    Column {
                                                        Text(
                                                            text = "Substitute Teacher",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = classInfo.teacherName,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                            )
                                                            Text(
                                                                text = " → ",
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                            Text(
                                                                text = classInfo.substituteTeacher ?: "Substitute",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.tertiary
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Column {
                                                        Text(
                                                            text = "Teacher",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = classInfo.teacherName,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Room Information
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Room,
                                                    contentDescription = "Room",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Location",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = classInfo.room,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    } ?: run {
                                        // Fallback if no class info available
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = "No Information",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "No class information available",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Today's class schedule grid (added)
                            if (homeState.currentPeriod != null || homeState.nextPeriod != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Today's Schedule",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                )
                                
                                // Create a grid of time slots
                                val periods = PeriodSettings.getPeriodsSettings(context)
                                val activePeriods = periods.filter { it.isActive }
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Header row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Period",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(0.2f)
                                            )
                                            Text(
                                                text = "Time",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(0.3f)
                                            )
                                            Text(
                                                text = "Class",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(0.5f)
                                            )
                                        }
                                        
                                        Divider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                        )
                                        
                                        // Period rows - using LazyColumn instead of forEach for Composables
                                        Column {
                                            for (period in activePeriods) {
                                                val isCurrentPeriod = period.periodNumber == homeState.currentPeriod
                                                val isNextPeriod = period.periodNumber == homeState.nextPeriod
                                                
                                                PeriodScheduleRow(
                                                    period = period,
                                                    isCurrentPeriod = isCurrentPeriod,
                                                    isNextPeriod = isNextPeriod,
                                                    currentClassInfo = if (isCurrentPeriod) homeState.currentClassInfo else null,
                                                    nextClassInfo = if (isNextPeriod) homeState.nextClassInfo else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (homeState.nextPeriod != null) {
                        // No current period, but next period exists
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${homeState.nextPeriod}",
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Text(
                                            text = "Next Period",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val startTime = homeState.nextPeriodTimeRange.split("-")[0].trim()
                                        Text(
                                            text = "Starting at $startTime",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    // Upcoming badge
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Text(
                                            text = "UPCOMING",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                }
                                
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                                )
                                
                                // Countdown to next period
                                if (homeState.nextPeriodTimeRange.isNotEmpty()) {
                                    val startTimeParts = homeState.nextPeriodTimeRange.split("-")[0].trim().split(":")
                                    if (startTimeParts.size == 2) {
                                        val startHour = startTimeParts[0].toIntOrNull() ?: 0
                                        val startMinute = startTimeParts[1].toIntOrNull() ?: 0
                                        val now = LocalTime.now()
                                        
                                        // Calculate minutes until next period
                                        val startTimeObj = LocalTime.of(startHour, startMinute)
                                        val minutesUntil = if (startTimeObj.isAfter(now)) {
                                            (startTimeObj.hour - now.hour) * 60 + (startTimeObj.minute - now.minute)
                                        } else {
                                            0
                                        }
                                        
                                        if (minutesUntil > 0) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Timelapse,
                                                        contentDescription = "Time Remaining",
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Starts in $minutesUntil minute${if (minutesUntil != 1) "s" else ""}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                                
                                // Display upcoming class information
                                val nextPeriodNumber = homeState.nextPeriod
                                if (nextPeriodNumber != null) {
                                    // Use real class information from the HomeViewModel
                                    val nextClassInfo = homeState.nextClassInfo
                                    
                                    if (nextClassInfo != null) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.School,
                                                        contentDescription = "Subject",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = nextClassInfo.subject,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = nextClassInfo.grade,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = "Teacher",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = if (nextClassInfo.isSubstituted) 
                                                            MaterialTheme.colorScheme.tertiary 
                                                        else 
                                                            MaterialTheme.colorScheme.secondary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    if (nextClassInfo.isSubstituted) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = nextClassInfo.teacherName,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                            )
                                                            Text(
                                                                text = " → ${nextClassInfo.substituteTeacher}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.tertiary,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                            
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            
                                                            Badge(
                                                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                                                contentColor = MaterialTheme.colorScheme.tertiary
                                                            ) {
                                                                Text("SUB", 
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Text(
                                                            text = nextClassInfo.teacherName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Room,
                                                        contentDescription = "Room",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = nextClassInfo.room,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Fallback message when no class information is available
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = "No Information",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "No class information available for period ${nextPeriodNumber}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Text(
                                    text = "You have a break until the next period starts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        // No current or next period
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
                                    Icons.Default.EventBusy,
                                    contentDescription = "No Classes",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "No Active Classes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "There are no classes scheduled at this time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { navController.navigate(Screen.Schedule.route) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("View Schedule")
                        Spacer(modifier = Modifier.width(4.dp))
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
private fun PeriodScheduleRow(
    period: com.substituemanagment.managment.data.PeriodSetting,
    isCurrentPeriod: Boolean,
    isNextPeriod: Boolean,
    currentClassInfo: ClassInfo?,
    nextClassInfo: ClassInfo?
) {
    Surface(
        color = when {
            isCurrentPeriod -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isNextPeriod -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Period number
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrentPeriod -> MaterialTheme.colorScheme.primary
                            isNextPeriod -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${period.periodNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrentPeriod -> MaterialTheme.colorScheme.onPrimary
                        isNextPeriod -> MaterialTheme.colorScheme.onSecondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Time range
            Text(
                text = period.formatTimeRange(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.3f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Class info
            val classInfo = when {
                isCurrentPeriod -> currentClassInfo
                isNextPeriod -> nextClassInfo
                else -> null
            }
            
            Row(
                modifier = Modifier.weight(0.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (classInfo != null) {
                    Column {
                        Text(
                            text = classInfo.subject,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (classInfo.isSubstituted) {
                                Text(
                                    text = "Sub: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Text(
                                text = if (classInfo.isSubstituted) 
                                    (classInfo.substituteTeacher ?: "Substitute") 
                                else 
                                    classInfo.teacherName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (classInfo.isSubstituted)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (isCurrentPeriod) {
                        Spacer(modifier = Modifier.weight(1f))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp
                            )
                        }
                    } else if (isNextPeriod) {
                        Spacer(modifier = Modifier.weight(1f))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Text(
                                text = "NEXT",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
} 