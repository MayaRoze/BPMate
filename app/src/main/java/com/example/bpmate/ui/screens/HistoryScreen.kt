package com.example.bpmate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onActivityClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = viewModel()
) {
    val history by viewModel.activityHistory.collectAsState()
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No activities recorded yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(history) { item ->
                    val activity = item.activity
                    ListItem(
                        modifier = Modifier.clickable { onActivityClick(activity.id) },
                        headlineContent = { Text(activity.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { 
                            val dateString = dateFormatter.format(Date(activity.startTime))
                            Text("$dateString • ${formatTime(activity.duration)}") 
                        },
                        trailingContent = { 
                            Text(activity.playlistName, style = MaterialTheme.typography.bodySmall) 
                        }
                    )
                    HorizontalDivider()
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
