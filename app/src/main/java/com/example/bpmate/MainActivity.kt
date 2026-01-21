package com.example.bpmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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
    // Create a shared PlaybackViewModel scoped to the Activity/App level
    val playbackViewModel: PlaybackViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartNewActivity = { navController.navigate("create_activity") },
                onViewHistory = { navController.navigate("history") },
                onManagePlaylists = { navController.navigate("playlists") }
            )
        }
        composable("playlists") {
            PlaylistManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("create_activity") {
            CreateActivityScreen(
                onStartRecording = { playlistId -> 
                    navController.navigate("player/$playlistId") 
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "player/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlayerScreen(
                playlistId = playlistId,
                onFinishActivity = { navController.navigate("analysis") },
                onBack = { navController.popBackStack() },
                playbackViewModel = playbackViewModel
            )
        }
        composable("analysis") {
            AnalysisScreen(
                onDone = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                } },
                playbackViewModel = playbackViewModel
            )
        }
        composable("history") {
            HistoryScreen(
                onActivityClick = { activityId ->
                    navController.navigate("analysis")
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
