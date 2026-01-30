package com.example.bpmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bpmate.data.PlayedSong
import com.example.bpmate.data.local.ActivityWithPlayedSongs
import com.example.bpmate.playback.PlaybackViewModel
import com.example.bpmate.ui.screens.PlaylistViewModel
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

    // Use historical data if available, otherwise fallback to current session data
    val displaySongs = historicalData?.playedSongs?.map { 
        PlayedSong(
            title = it.title, 
            artist = it.artist, 
            timestamp = it.timestamp,
            bpm = it.bpm,
            cadence = it.cadence
        ) 
    } ?: currentPlayedSongs

    val displayName = (historicalData?.activity?.name ?: currentName).ifBlank { "Activity Summary" }
    val displayDesc = historicalData?.activity?.description ?: currentDesc
    val displayPlaylist = historicalData?.activity?.playlistName ?: currentPlaylist
    val displayMode = historicalData?.activity?.mode ?: currentMode
    val displayStartTime = historicalData?.activity?.startTime ?: currentStartTime

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
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (displayDesc.isNotBlank()) {
                    Text(
                        text = displayDesc,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                Text(
                    text = "Song History", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (displaySongs.isEmpty()) {
                item {
                    Text("No songs recorded for this activity.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(displaySongs) { song ->
                    ListItem(
                        headlineContent = { Text(song.title) },
                        supportingContent = { 
                            Column {
                                Text(song.artist)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text("${song.bpm} BPM", fontSize = 11.sp) },
                                        enabled = false
                                    )
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text("${song.cadence} SPM", fontSize = 11.sp) },
                                        enabled = false
                                    )
                                }
                            }
                        },
                        trailingContent = { Text(formatTime(song.timestamp)) }
                    )
                    HorizontalDivider()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Home")
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
