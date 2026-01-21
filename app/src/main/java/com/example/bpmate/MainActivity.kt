package com.example.bpmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bpmate.ui.screens.AnalysisScreen
import com.example.bpmate.ui.screens.CreateActivityScreen
import com.example.bpmate.ui.screens.HistoryScreen
import com.example.bpmate.ui.screens.HomeScreen
import com.example.bpmate.ui.screens.PlayerScreen
import com.example.bpmate.ui.theme.BPMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BPMateTheme {
                BPMateApp()
            }
        }
    }
}

@Composable
fun BPMateApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartNewActivity = { navController.navigate("create_activity") },
                onViewHistory = { navController.navigate("history") }
            )
        }
        composable("create_activity") {
            CreateActivityScreen(
                onStartRecording = { navController.navigate("player") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("player") {
            PlayerScreen(
                onFinishActivity = { navController.navigate("analysis") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("analysis") {
            AnalysisScreen(
                onDone = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                } }
            )
        }
        composable("history") {
            HistoryScreen(
                onActivityClick = { activityId ->
                    navController.navigate("analysis") // In real app, pass activityId
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
