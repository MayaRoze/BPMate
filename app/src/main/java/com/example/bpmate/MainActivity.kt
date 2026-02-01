package com.example.bpmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bpmate.playback.PlaybackViewModel
import com.example.bpmate.ui.screens.*
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
    // Shared ViewModels at the App level to ensure consistent state and prevent redundant init logic
    val playbackViewModel: PlaybackViewModel = viewModel()
    val bluetoothViewModel: BluetoothViewModel = viewModel()
    val playlistViewModel: PlaylistViewModel = viewModel()
    val context = LocalContext.current

    // Initialize playback controller as soon as the app starts
    LaunchedEffect(Unit) {
        playbackViewModel.connectController(context)
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartNewActivity = { navController.navigate("create_activity") },
                onViewHistory = { navController.navigate("history") },
                onManagePlaylists = { navController.navigate("playlists") },
                onNavigateToDebug = { navController.navigate("imu_debug") }
            )
        }
        composable("playlists") {
            PlaylistManagementScreen(
                onBack = { navController.popBackStack() },
                viewModel = playlistViewModel
            )
        }
        composable("create_activity") {
            CreateActivityScreen(
                onStartRecording = { playlistId -> 
                    navController.navigate("player/$playlistId") 
                },
                onBack = { navController.popBackStack() },
                playlistViewModel = playlistViewModel,
                playbackViewModel = playbackViewModel
            )
        }
        composable(
            "player/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlayerScreen(
                playlistId = playlistId,
                onFinishActivity = { activityId -> 
                    navController.navigate("analysis?activityId=$activityId") 
                },
                onBack = { navController.popBackStack() },
                playlistViewModel = playlistViewModel,
                playbackViewModel = playbackViewModel,
                bluetoothViewModel = bluetoothViewModel
            )
        }
        composable(
            "analysis?activityId={activityId}",
            arguments = listOf(navArgument("activityId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null 
            })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")
            AnalysisScreen(
                activityId = activityId,
                onDone = { 
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    } 
                },
                playbackViewModel = playbackViewModel,
                playlistViewModel = playlistViewModel
            )
        }
        composable("history") {
            HistoryScreen(
                onActivityClick = { activityId ->
                    navController.navigate("analysis?activityId=$activityId")
                },
                onBack = { navController.popBackStack() },
                viewModel = playlistViewModel
            )
        }
        composable("imu_debug") {
            IMU_DebugScreen(
                onBack = { navController.popBackStack() },
                bluetoothViewModel = bluetoothViewModel
            )
        }
    }
}
