package com.example.bpmate.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistManagementScreen(
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = viewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    
    val selectedPlaylist = playlists.find { it.id == selectedPlaylistId }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            selectedPlaylistId?.let { id ->
                viewModel.addSongsToPlaylist(id, uris)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedPlaylist?.name ?: "My Playlists") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedPlaylistId != null) {
                            selectedPlaylistId = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedPlaylistId == null) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                }
            } else {
                FloatingActionButton(onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Songs")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedPlaylistId == null) {
                if (playlists.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No playlists yet. Tap + to create one.")
                    }
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("${playlist.songs.size} songs") },
                                trailingContent = {
                                    IconButton(onClick = {
                                        viewModel.deletePlaylist(playlist.id, playlist.name)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Playlist")
                                    }
                                },
                                modifier = Modifier.clickable { selectedPlaylistId = playlist.id }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                selectedPlaylist?.let { playlist ->
                    if (playlist.songs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No songs in this playlist. Tap + to add some.")
                        }
                    } else {
                        LazyColumn {
                            items(playlist.songs) { song ->
                                ListItem(
                                    headlineContent = { Text(song.title) },
                                    supportingContent = { Text(song.artist) },
                                    leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            viewModel.removeSong(
                                                song.id, 
                                                playlist.id, 
                                                song.title, 
                                                song.artist, 
                                                song.uri.toString()
                                            )
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.createPlaylist(newName)
                        showCreateDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
