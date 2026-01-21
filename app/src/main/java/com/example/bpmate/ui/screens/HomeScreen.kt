package com.example.bpmate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartNewActivity: () -> Unit,
    onViewHistory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("BPMate") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to BPMate",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onStartNewActivity,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Start an Activity")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Activity History")
            }
        }
    }
}
