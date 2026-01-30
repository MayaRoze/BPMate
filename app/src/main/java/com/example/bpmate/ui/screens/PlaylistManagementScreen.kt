package com.example.bpmate.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bpmate.data.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistManagementScreen(
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = viewModel()
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var editingSong by remember { mutableStateOf<Song?>(null) }

    val selectedPlaylist = playlists.find { it.id == selectedPlaylistId }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            selectedPlaylistId?.let { id ->
                uris.forEach { uri ->
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                                        leadingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable { editingSong = song }
                                            ) {
                                                Icon(Icons.Default.MusicNote, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("${song.bpm} BPM")
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit BPM",
                                                    modifier = Modifier.size(16.dp).padding(start = 4.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        trailingContent = {
                                            IconButton(onClick = {
                                                viewModel.removeSong(
                                                    song.id,
                                                    playlist.id,
                                                    song.title,
                                                    song.artist,
                                                    song.uri.toString(),
                                                    song.bpm
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
            if (selectedPlaylistId == null) {
                val uriHandler = LocalUriHandler.current
                Text(
                    text = "BPM data provided by getsongbpm.com",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clickable { uriHandler.openUri("https://getsongbpm.com") },
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary
                )
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

    editingSong?.let { song ->
        var bpmText by remember { mutableStateOf(song.bpm.toString()) }
        AlertDialog(
            onDismissRequest = { editingSong = null },
            title = { Text("Edit BPM - ${song.title}") },
            text = {
                Column {
                    Text("Enter the correct BPM for this song:")
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = bpmText,
                        onValueChange = { bpmText = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("BPM") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newBpm = bpmText.toIntOrNull() ?: song.bpm
                    selectedPlaylistId?.let { pid ->
                        viewModel.updateSongBpm(
                            song.id,
                            pid,
                            song.title,
                            song.artist,
                            song.uri.toString(),
                            newBpm
                        )
                    }
                    editingSong = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSong = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
