package com.example.bpmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bpmate.playback.PlaybackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onDone: () -> Unit,
    playbackViewModel: PlaybackViewModel = viewModel()
) {
    val playedSongs by playbackViewModel.playedSongs.collectAsState()

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
                    text = "Activity Summary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val totalDuration = if (playedSongs.isNotEmpty()) {
                    formatTime(playedSongs.last().timestamp)
                } else "0:00"
                Text("Total Duration: $totalDuration", style = MaterialTheme.typography.bodyMedium)
            }

            item {
                Text("Performance Graph", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cadence Graph Placeholder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Text("Song History", style = MaterialTheme.typography.titleLarge)
            }

            if (playedSongs.isEmpty()) {
                item {
                    Text("No songs played during this activity.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(playedSongs) { song ->
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
