package com.substituemanagment.managment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.substituemanagment.managment.navigation.NavGraph
import com.substituemanagment.managment.ui.components.BottomNav
import com.substituemanagment.managment.ui.theme.SubstitutionManagementTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SubstitutionManagementTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNav(navController) }
                ) { paddingValues ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}