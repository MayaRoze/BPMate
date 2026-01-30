package com.example.bpmate.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bpmate.R
import com.example.bpmate.playback.PlaybackViewModel
import com.example.bpmate.ui.theme.TranslucentDarkCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityScreen(
    onStartRecording: (String) -> Unit,
    onBack: () -> Unit,
    playlistViewModel: PlaylistViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var selectedPlaylistName by remember { mutableStateOf("Select Playlist") }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var movementMode by remember { mutableStateOf("Walk/Run") }
    var description by remember { mutableStateOf("") }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val playlists by playlistViewModel.playlists.collectAsState()
    val movementOptions = listOf("Walk/Run", "Drive")

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
                    title = { Text("New Activity", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Field Container
                item {
                    Surface(
                        color = TranslucentDarkCyan.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Activity Name", color = Color.White.copy(alpha = 0.8f)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                // Playlist Selector Container
                item {
                    Surface(
                        color = TranslucentDarkCyan.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Selected Playlist", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedCard(
                                onClick = { showPlaylistDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedPlaylistName, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Movement Mode Container
                item {
                    Surface(
                        color = TranslucentDarkCyan.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp).selectableGroup()) {
                            Text("Movement Mode", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                            movementOptions.forEach { text ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .selectable(
                                            selected = (text == movementMode),
                                            onClick = { movementMode = text },
                                            role = Role.RadioButton
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (text == movementMode),
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary, unselectedColor = Color.White.copy(alpha = 0.6f))
                                    )
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Description Container
                item {
                    Surface(
                        color = TranslucentDarkCyan.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description (Optional)", color = Color.White.copy(alpha = 0.8f)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                // Start Button Container
                item {
                    Button(
                        onClick = { 
                            val playlist = playlists.find { it.id == selectedPlaylistId }
                            if (playlist != null) {
                                playbackViewModel.startNewActivity(name, description, movementMode, playlist)
                                onStartRecording(playlist.id) 
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        enabled = name.isNotBlank() && selectedPlaylistId != null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Start Recording", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Select Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists found. Create one in 'Manage Playlists'.")
                } else {
                    Column {
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    selectedPlaylistName = playlist.name
                                    selectedPlaylistId = playlist.id
                                    showPlaylistDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${playlist.name} (${playlist.songs.size} songs)")
                            }
                        }
                    }
                }
            }
        )
    }
}
