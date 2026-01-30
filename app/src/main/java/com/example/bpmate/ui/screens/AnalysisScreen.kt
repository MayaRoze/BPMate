package com.example.bpmate.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bpmate.R
import com.example.bpmate.data.PlayedSong
import com.example.bpmate.data.local.ActivityWithPlayedSongs
import com.example.bpmate.playback.PlaybackViewModel
import com.example.bpmate.ui.screens.PlaylistViewModel
import com.example.bpmate.ui.theme.TranslucentDarkCyan
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

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_home),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Activity Analysis", fontWeight = FontWeight.Bold, color = Color.White) },
                    actions = {
                        IconButton(onClick = onDone) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = TranslucentDarkCyan.copy(alpha = 0.6f))
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        color = TranslucentDarkCyan.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (displayDesc.isNotBlank()) {
                                Text(
                                    text = displayDesc,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Date: $startDateString", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Mode: $displayMode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Playlist: $displayPlaylist", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            val totalDuration = if (displaySongs.isNotEmpty()) {
                                formatTime(displaySongs.last().timestamp)
                            } else "0:00"
                            Text("Total Duration: $totalDuration", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                item {
                    Surface(
                        color = TranslucentDarkCyan.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Song History", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                if (displaySongs.isEmpty()) {
                    item {
                        Text("No songs recorded for this activity.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                } else {
                    items(displaySongs) { song ->
                        Surface(
                            color = TranslucentDarkCyan.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { Text(song.title, color = Color.White, fontWeight = FontWeight.Bold) },
                                supportingContent = { 
                                    Column {
                                        Text(song.artist, color = Color.White.copy(alpha = 0.8f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${song.bpm} BPM",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${song.cadence} SPM",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }
                                },
                                trailingContent = { 
                                    Text(
                                        formatTime(song.timestamp), 
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) 
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Back to Home", fontWeight = FontWeight.Bold)
                    }
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
