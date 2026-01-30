package com.example.bpmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.example.bpmate.playback.PlaybackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playlistId: String,
    onFinishActivity: () -> Unit,
    onBack: () -> Unit,
    playlistViewModel: PlaylistViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(),
    bluetoothViewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val playlists by playlistViewModel.playlists.collectAsState()
    val player by playbackViewModel.player.collectAsState()
    val cadence by bluetoothViewModel.cadence.collectAsState()
    
    var currentTitle by remember { mutableStateOf("Not Playing") }
    var currentArtist by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // Connect to playback service and load playlist
    LaunchedEffect(Unit) {
        playbackViewModel.connectController(context)
    }

    // When player is ready and playlist is found, set it up
    LaunchedEffect(player, playlists) {
        val p = player
        val playlist = playlists.find { it.id == playlistId }
        if (p != null && playlist != null && p.mediaItemCount == 0) {
            playbackViewModel.setPlaylist(playlist)
        }
    }

    // Update UI based on player state
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        val listener = object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                currentTitle = mediaMetadata.title?.toString() ?: "Unknown"
                currentArtist = mediaMetadata.artist?.toString() ?: "Unknown Artist"
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_POSITION_DISCONTINUITY)) {
                    duration = player.duration.coerceAtLeast(0L)
                }
            }
        }
        p.addListener(listener)
        
        try {
            // Polling for position
            while (true) {
                position = p.currentPosition.coerceAtLeast(0L)
                kotlinx.coroutines.delay(1000)
            }
        } finally {
            p.removeListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording Activity") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Cadence Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CADENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "%.0f".format(cadence),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "steps / min",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = currentTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = currentArtist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                onValueChange = { /* Seek if needed */ },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(position), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(onClick = { player?.seekToPreviousMediaItem() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(40.dp))
                }
                FilledIconButton(
                    onClick = { 
                        val p = player ?: return@FilledIconButton
                        if (p.isPlaying) p.pause() else p.play()
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { player?.seekToNextMediaItem() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    playbackViewModel.stopAndSaveActivity { activityId ->
                        onFinishActivity()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop and Save Activity")
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
