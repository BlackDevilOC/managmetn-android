package com.substituemanagment.managment.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.substituemanagment.managment.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BottomNav(navController: NavController) {
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Animation variables
    var lastSelectedItemIndex by remember { mutableStateOf(0) }
    val selectedItemIndex = when (currentRoute) {
        Screen.Home.route -> 0
        Screen.Teachers.route -> 1
        Screen.Assign.route -> 2
        Screen.SmsSend.route -> 3
        Screen.Settings.route -> 4
        else -> lastSelectedItemIndex
    }
    
    if (selectedItemIndex != lastSelectedItemIndex) {
        lastSelectedItemIndex = selectedItemIndex
    }

    NavigationBar {
        val items = listOf(
            Triple(Screen.Home, "Home", Icons.Default.Home),
            Triple(Screen.Teachers, "Teachers", Icons.Default.Person),
            Triple(Screen.Assign, "Assign", Icons.Default.AssignmentInd),
            Triple(Screen.SmsSend, "SMS", Icons.Default.Send),
            Triple(Screen.Settings, "Settings", Icons.Default.Settings)
        )

        items.forEachIndexed { index, (screen, label, icon) ->
            val selected = currentRoute == screen.route
            val animatedScale = remember { Animatable(1f) }
            
            // Bounce animation on selection
            LaunchedEffect(selected) {
                if (selected) {
                    animatedScale.animateTo(
                        targetValue = 0.85f,
                        animationSpec = tween(
                            durationMillis = 150,
                            easing = FastOutSlowInEasing
                        )
                    )
                    animatedScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
            
            NavigationBarItem(
                icon = { 
                    Icon(
                        icon, 
                        contentDescription = if (screen == Screen.Assign) "Assign Substitutes" else label,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = animatedScale.value
                                scaleY = animatedScale.value
                            }
                    ) 
                },
                label = { Text(label) },
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        // Always navigate to home first to reset the navigation state
                        if (screen.route != Screen.Home.route) {
                            scope.launch {
                                // Navigate to Home first and then to the desired screen with slight delay
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                                delay(10) // Small delay for the home screen to register
                                navController.navigate(screen.route)
                            }
                        } else {
                            // If home is clicked, just navigate to home and clear back stack
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    } else if (screen.route == Screen.Home.route) {
                        // If already on home and home is clicked again, reset to fresh home state
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
} 