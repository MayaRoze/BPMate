package com.example.bpmate.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.bpmate.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartNewActivity: () -> Unit,
    onViewHistory: () -> Unit,
    onManagePlaylists: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.background_home),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 1f
        )

        Scaffold(
            containerColor = Color.Transparent, // Make Scaffold transparent to see background
            topBar = {
                TopAppBar(
                    title = { Text("BPMate", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                // Primary action: High emphasis
                Button(
                    onClick = onStartNewActivity,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Start an Activity", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Secondary actions: Medium emphasis with semi-transparent background
                // Content color set to a solid, opaque onSurface color
                val secondaryButtonColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f)
                )

                Button(
                    onClick = onManagePlaylists,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = secondaryButtonColors
                ) {
                    Text(
                        text = "Manage Playlists",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onViewHistory,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = secondaryButtonColors
                ) {
                    Text(
                        text = "Activity History",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onNavigateToDebug,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = secondaryButtonColors
                ) {
                    Text(
                        text = "IMU Debug",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}
