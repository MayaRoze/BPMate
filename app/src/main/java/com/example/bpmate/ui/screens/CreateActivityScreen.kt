package com.example.bpmate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bpmate.playback.PlaybackViewModel

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Activity Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedCard(
                onClick = { showPlaylistDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedPlaylistName, modifier = Modifier.weight(1.0f))
                }
            }

            Column(Modifier.selectableGroup()) {
                Text("Movement Mode", style = MaterialTheme.typography.labelLarge)
                movementOptions.forEach { text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (text == movementMode),
                                onClick = { movementMode = text },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == movementMode),
                            onClick = null
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    val playlist = playlists.find { it.id == selectedPlaylistId }
                    if (playlist != null) {
                        playbackViewModel.startNewActivity(name, description, movementMode, playlist)
                        onStartRecording(playlist.id) 
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && selectedPlaylistId != null
            ) {
                Text("Start Recording")
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
