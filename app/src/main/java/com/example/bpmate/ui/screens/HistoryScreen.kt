package com.example.bpmate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onActivityClick: (String) -> Unit,
    onBack: () -> Unit
) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(historyData) { activity ->
                ListItem(
                    modifier = Modifier.clickable { onActivityClick(activity.id) },
                    headlineContent = { Text(activity.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${activity.date} • ${activity.duration}") },
                    trailingContent = { Text(activity.mode) }
                )
                HorizontalDivider()
            }
        }
    }
}

data class ActivityHistoryItem(
    val id: String,
    val name: String,
    val date: String,
    val duration: String,
    val mode: String
)

val historyData = listOf(
    ActivityHistoryItem("1", "Morning Run", "Oct 24, 2023", "25:34", "Walk/Run"),
    ActivityHistoryItem("2", "Commute to Work", "Oct 23, 2023", "45:10", "Drive"),
    ActivityHistoryItem("3", "Evening Jog", "Oct 21, 2023", "30:15", "Walk/Run"),
    ActivityHistoryItem("4", "Grocery Trip", "Oct 20, 2023", "15:20", "Drive"),
    ActivityHistoryItem("5", "Weekend Hike", "Oct 19, 2023", "1:20:45", "Walk/Run")
)
