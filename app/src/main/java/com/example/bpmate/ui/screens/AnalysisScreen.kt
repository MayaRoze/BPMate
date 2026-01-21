package com.example.bpmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bpmate.data.PlayedSong
import com.example.bpmate.data.local.ActivityWithPlayedSongs
import com.example.bpmate.playback.PlaybackViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    activityId: String? = null,
    onDone: () -> Unit,
    playbackViewModel: PlaybackViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel()
) {
    // Collect state from ViewModel for current session
    val currentPlayedSongs by playbackViewModel.playedSongs.collectAsState()
    val currentName by playbackViewModel.activityName.collectAsState()
    val currentDesc by playbackViewModel.activityDescription.collectAsState()
    val currentPlaylist by playbackViewModel.playlistName.collectAsState()
    val currentMode by playbackViewModel.activityMode.collectAsState()
    val currentStartTime by playbackViewModel.startTimeMillis.collectAsState()

    // State for historical data if viewing from history
    var historicalData by remember { mutableStateOf<ActivityWithPlayedSongs?>(null) }
    val history by playlistViewModel.activityHistory.collectAsState()

    // Determine which data to show
    LaunchedEffect(activityId, history) {
        if (activityId != null) {
            historicalData = history.find { it.activity.id == activityId }
        }
    }

    val displaySongs = if (activityId != null) {
        historicalData?.playedSongs?.map { PlayedSong(it.title, it.artist, it.timestamp) } ?: emptyList()
    } else {
        currentPlayedSongs
    }

    val displayName = if (activityId != null) historicalData?.activity?.name ?: "" else currentName
    val displayDesc = if (activityId != null) historicalData?.activity?.description ?: "" else currentDesc
    val displayPlaylist = if (activityId != null) historicalData?.activity?.playlistName ?: "" else currentPlaylist
    val displayMode = if (activityId != null) historicalData?.activity?.mode ?: "" else currentMode
    val displayStartTime = if (activityId != null) historicalData?.activity?.startTime ?: 0L else currentStartTime

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
    val startDateString = if (displayStartTime > 0) dateFormatter.format(Date(displayStartTime)) else ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Analysis") },
                actions = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Check, contentDescription = "Done")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = displayName.ifBlank { "Activity Summary" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (displayDesc.isNotBlank()) {
                    Text(
                        text = displayDesc,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Date: $startDateString", style = MaterialTheme.typography.bodyMedium)
                        Text("Mode: $displayMode", style = MaterialTheme.typography.bodyMedium)
                        Text("Playlist: $displayPlaylist", style = MaterialTheme.typography.bodyMedium)
                        val totalDuration = if (displaySongs.isNotEmpty()) {
                            formatTime(displaySongs.last().timestamp)
                        } else "0:00"
                        Text("Total Duration: $totalDuration", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Text("Song History", style = MaterialTheme.typography.titleLarge)
            }

            if (displaySongs.isEmpty()) {
                item {
                    Text("No songs recorded for this activity.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(displaySongs) { song ->
                    ListItem(
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artist) },
                        trailingContent = { Text(formatTime(song.timestamp)) }
                    )
                    HorizontalDivider()
                }
            }

            item {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
