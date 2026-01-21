package com.example.bpmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onDone: () -> Unit
) {
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
                    text = "Morning Run",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A quick run around the park.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Duration: 25:34", style = MaterialTheme.typography.bodyMedium)
                Text("Mode: Walk/Run", style = MaterialTheme.typography.bodyMedium)
            }

            item {
                Text("Performance Graph", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                // Placeholder for Graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cadence/Speed Graph Placeholder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Text("Song History", style = MaterialTheme.typography.titleLarge)
            }

            items(songHistory) { song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(song.artist) },
                    trailingContent = { Text(song.time) }
                )
                HorizontalDivider()
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

data class SongInfo(val title: String, val artist: String, val time: String)

val songHistory = listOf(
    SongInfo("Walking on Sunshine", "Katrina & The Waves", "0:00"),
    SongInfo("Eye of the Tiger", "Survivor", "3:58"),
    SongInfo("Run to the Hills", "Iron Maiden", "8:05"),
    SongInfo("Born to Run", "Bruce Springsteen", "12:00"),
    SongInfo("Fast Car", "Tracy Chapman", "16:45")
)
