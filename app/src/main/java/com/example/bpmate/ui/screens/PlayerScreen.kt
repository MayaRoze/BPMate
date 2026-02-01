package com.example.bpmate.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaMetadataRetriever
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.example.bpmate.R
import com.example.bpmate.playback.PlaybackViewModel
import com.example.bpmate.ui.theme.TranslucentDarkCyan
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PlayerScreen(
    playlistId: String,
    onFinishActivity: (String) -> Unit,
    onBack: () -> Unit,
    playlistViewModel: PlaylistViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(),
    bluetoothViewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val playlists by playlistViewModel.playlists.collectAsState()
    val player by playbackViewModel.player.collectAsState()
    val cadence by bluetoothViewModel.cadence.collectAsState()
    val connectionStatus by bluetoothViewModel.connectionStatus.collectAsState()
    val activityMode by playbackViewModel.activityMode.collectAsState()
    
    var currentTitle by remember { mutableStateOf("Not Playing") }
    var currentArtist by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    
    var currentSpeedKmh by remember { mutableStateOf(0f) }
    var artwork by remember { mutableStateOf<ImageBitmap?>(null) }

    // Location Permission for Driving Mode
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    // Handle system back button
    BackHandler {
        playbackViewModel.stopPlayback()
        onBack()
    }

    // Connect to playback service and load playlist
    LaunchedEffect(Unit) {
        playbackViewModel.connectController(context)
    }

    // GPS Tracking for Driving Mode
    LaunchedEffect(activityMode, locationPermissions.allPermissionsGranted) {
        if (activityMode == "Drive" && locationPermissions.allPermissionsGranted) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val speedKmh = location.speed * 3.6f
                    currentSpeedKmh = speedKmh
                    playbackViewModel.updateVelocity(speedKmh)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener)
            } catch (e: Exception) {
                // Fallback if GPS fails
            }
        }
    }

    // Request permissions if needed
    LaunchedEffect(activityMode) {
        if (activityMode == "Drive" && !locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Sync cadence with playbackViewModel for smart song selection
    LaunchedEffect(cadence) {
        playbackViewModel.updateCadence(cadence)
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
        
        val updateMetadata = {
            currentTitle = p.mediaMetadata.title?.toString() ?: "Unknown"
            currentArtist = p.mediaMetadata.artist?.toString() ?: "Unknown Artist"
            isPlaying = p.isPlaying
            duration = p.duration.coerceAtLeast(0L)
        }
        updateMetadata()

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
            while (true) {
                position = p.currentPosition.coerceAtLeast(0L)
                kotlinx.coroutines.delay(1000)
            }
        } finally {
            p.removeListener(listener)
        }
    }

    // Dynamic Artwork Extraction
    LaunchedEffect(player, currentTitle) {
        val p = player ?: return@LaunchedEffect
        val metadata = p.mediaMetadata
        val data = metadata.artworkData
        
        if (data != null) {
            artwork = BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
        } else {
            val uri = p.currentMediaItem?.localConfiguration?.uri
            if (uri != null) {
                artwork = withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        if (uri.scheme == "asset") {
                            val path = uri.path?.removePrefix("/") ?: ""
                            val afd = context.assets.openFd(path)
                            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                        } else {
                            retriever.setDataSource(context, uri)
                        }
                        retriever.embeddedPicture?.let { bytes ->
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        }
                    } catch (e: Exception) {
                        null
                    } finally {
                        retriever.release()
                    }
                }
            } else {
                artwork = null
            }
        }
    }

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
                    title = { Text("Recording Activity", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = {
                            playbackViewModel.stopPlayback()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                playbackViewModel.stopAndSaveActivity { activityId ->
                                    onFinishActivity(activityId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.padding(end = 8.dp).height(40.dp)
                        ) {
                            Text("STOP", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = TranslucentDarkCyan.copy(alpha = 0.6f))
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Movement Data Display (Cadence or Velocity)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val label = if (activityMode == "Drive") "VELOCITY" else "CADENCE"
                        val value = if (activityMode == "Drive") "%.0f".format(currentSpeedKmh) else "%.0f".format(cadence)
                        val unit = if (activityMode == "Drive") "km / h" else "steps / min"
                        val icon = if (activityMode == "Drive") Icons.Default.DirectionsCar else Icons.Default.DirectionsWalk

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 64.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        if (activityMode != "Drive" && connectionStatus != "Connected") {
                            Text(
                                text = "Sensor: $connectionStatus",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else if (activityMode == "Drive" && !locationPermissions.allPermissionsGranted) {
                            Text(
                                text = "Location permission required",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (artwork != null) {
                        Image(
                            bitmap = artwork!!,
                            contentDescription = "Cover Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    color = TranslucentDarkCyan.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    initialDelayMillis = 700,
                                    repeatDelayMillis = 700
                                )
                        )

                        Text(
                            text = currentArtist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { /* Seek if needed */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(onClick = { player?.seekToPreviousMediaItem() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(40.dp), tint = Color.White)
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
                    IconButton(onClick = { playbackViewModel.skipToBestMatch() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next (Auto-BPM)", modifier = Modifier.size(40.dp), tint = Color.White)
                    }
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
