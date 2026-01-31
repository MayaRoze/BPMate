package com.example.bpmate.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.example.bpmate.data.PlayedSong
import com.example.bpmate.data.Playlist
import com.example.bpmate.data.Song
import com.example.bpmate.data.local.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).playlistDao()
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _player = MutableStateFlow<Player?>(null)
    val player = _player.asStateFlow()

    private val _playedSongs = MutableStateFlow<List<PlayedSong>>(emptyList())
    val playedSongs = _playedSongs.asStateFlow()

    // Activity Metadata
    private val _activityName = MutableStateFlow("")
    val activityName = _activityName.asStateFlow()

    private val _activityDescription = MutableStateFlow("")
    val activityDescription = _activityDescription.asStateFlow()

    private val _playlistName = MutableStateFlow("")
    val playlistName = _playlistName.asStateFlow()

    private val _activityMode = MutableStateFlow("Walk/Run")
    val activityMode = _activityMode.asStateFlow()

    private val _startTimeMillis = MutableStateFlow(0L)
    val startTimeMillis = _startTimeMillis.asStateFlow()

    private var activityStartTime: Long = 0
    private var currentPlaylist: Playlist? = null
    private var currentCadence: Float = 0f
    private var currentVelocity: Float = 0f // in km/h

    // History to prevent repetition
    private val playedHistory = mutableListOf<String>()
    private val HISTORY_FRACTION = 0.3f // 30% of playlist size or at least 1

    fun connectController(context: Context) {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val p = controllerFuture?.get()
                _player.value = p
                setupMediaListener(p)
            } catch (e: Exception) {
                Log.e("PlaybackViewModel", "Failed to connect controller", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupMediaListener(player: Player?) {
        player?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Maintain the 2-item dynamic queue whenever we transition to a new song
                // (Except for cases where we transition away from the playlist entirely)
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    pickNextSongBasedOnTarget()
                }

                mediaItem?.let { item ->
                    val metadata = item.mediaMetadata
                    val title = metadata.title?.toString() ?: "Unknown"
                    val artist = metadata.artist?.toString() ?: "Unknown Artist"
                    val timestamp = if (activityStartTime == 0L) 0L else System.currentTimeMillis() - activityStartTime
                    
                    // Add to history to prevent immediate repeats
                    addToHistory(item.mediaId)

                    // Track session statistics
                    val songInPlaylist = currentPlaylist?.songs?.find { it.id == item.mediaId }
                    val songBpm = songInPlaylist?.bpm ?: 0
                    val recordedMovementValue = if (_activityMode.value == "Drive") currentVelocity.toInt() else currentCadence.toInt()

                    val currentList = _playedSongs.value
                    if (currentList.lastOrNull()?.let { it.title == title && it.timestamp == timestamp } != true) {
                        _playedSongs.value = currentList + PlayedSong(
                            title = title,
                            artist = artist,
                            timestamp = timestamp,
                            bpm = songBpm,
                            cadence = recordedMovementValue
                        )
                    }
                }
            }
        })
    }

    private fun addToHistory(mediaId: String?) {
        if (mediaId == null || mediaId.isBlank()) return
        playedHistory.remove(mediaId) // Move to end if exists
        playedHistory.add(mediaId)
        
        val maxHistory = getMaxHistorySize()
        while (playedHistory.size > maxHistory) {
            playedHistory.removeAt(0)
        }
    }

    private fun getMaxHistorySize(): Int {
        val playlistSize = currentPlaylist?.songs?.size ?: 0
        if (playlistSize <= 1) return 0
        // Blacklist up to 30% of the playlist, but always leave at least one song available besides current
        return (playlistSize * HISTORY_FRACTION).toInt().coerceAtLeast(1).coerceAtMost(playlistSize - 1)
    }

    fun updateCadence(cadence: Float) {
        if (_activityMode.value == "Walk/Run") {
            val oldCadence = currentCadence
            currentCadence = cadence
            if (abs(oldCadence - cadence) > 5f) {
                pickNextSongBasedOnTarget()
            }
        }
    }

    fun updateVelocity(velocityKmH: Float) {
        if (_activityMode.value == "Drive") {
            val oldVelocity = currentVelocity
            currentVelocity = velocityKmH
            if (abs(oldVelocity - velocityKmH) > 10f) {
                pickNextSongBasedOnTarget()
            }
        }
    }

    private fun getTargetBpm(): Int {
        return if (_activityMode.value == "Drive") {
            (60f + currentVelocity).toInt().coerceIn(60, 180)
        } else {
            currentCadence.toInt()
        }
    }

    /**
     * Maintains a stable 2-item queue [current, next].
     * This avoids the infinite queue growth and the "stuck" skip bug.
     */
    private fun pickNextSongBasedOnTarget() {
        val p = _player.value ?: return
        val playlist = currentPlaylist ?: return
        val targetBpm = getTargetBpm()
        
        if (playlist.songs.isEmpty() || targetBpm <= 0) return

        // 1. Cleanup: Remove any previous songs to keep current song at index 0
        while (p.currentMediaItemIndex > 0) {
            p.removeMediaItem(0)
        }

        // 2. Select the absolute best next song based on current pace
        val currentMediaId = p.currentMediaItem?.mediaId ?: ""
        
        // Candidates: Exclude current song and the recently played history
        val candidates = playlist.songs.filter { it.id != currentMediaId && !playedHistory.contains(it.id) }
        
        val nextSong = if (candidates.isNotEmpty()) {
            candidates.minByOrNull { abs(it.bpm - targetBpm) }
        } else {
            // Fallback: exclude only current if history is too restrictive
            playlist.songs.filter { it.id != currentMediaId }.minByOrNull { abs(it.bpm - targetBpm) }
        }

        if (nextSong != null) {
            val nextItem = createMediaItem(nextSong)
            if (p.mediaItemCount > 1) {
                // Replace the existing "next" item if it's no longer the best match
                if (p.getMediaItemAt(1).mediaId != nextSong.id) {
                    p.replaceMediaItem(1, nextItem)
                }
            } else {
                // If queue has only the current item, append the next one
                p.addMediaItem(nextItem)
            }
            
            // 3. Ensure no trailing items exist
            while (p.mediaItemCount > 2) {
                p.removeMediaItem(2)
            }
            Log.d("PlaybackViewModel", "Queue optimized [0: current, 1: ${nextSong.title}]")
        }
    }

    /**
     * Skips to the current best match and immediately prepares the next one.
     */
    fun skipToBestMatch() {
        val p = _player.value ?: return
        
        // 1. Re-calculate the best match for the LATEST pace
        pickNextSongBasedOnTarget()
        
        // 2. Skip forward. If the next item was just added, seekToNext handles it.
        if (p.hasNextMediaItem()) {
            p.seekToNextMediaItem()
        }
    }

    private fun createMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .build()
            )
            .build()
    }

    fun startNewActivity(name: String, description: String, mode: String, playlist: Playlist) {
        _activityName.value = name
        _activityDescription.value = description
        _activityMode.value = mode
        _playlistName.value = playlist.name
        _startTimeMillis.value = System.currentTimeMillis()
        setPlaylist(playlist)
    }

    fun setPlaylist(playlist: Playlist) {
        val p = _player.value ?: return
        currentPlaylist = playlist
        activityStartTime = System.currentTimeMillis()
        _playedSongs.value = emptyList()
        playedHistory.clear()
        
        if (_playlistName.value.isBlank()) {
            _playlistName.value = playlist.name
            _startTimeMillis.value = System.currentTimeMillis()
        }

        // Initialize fresh state
        p.stop()
        p.clearMediaItems()
        p.shuffleModeEnabled = false
        p.repeatMode = Player.REPEAT_MODE_OFF

        // Pick first song
        val targetBpm = getTargetBpm()
        val startSong = if (targetBpm > 0) {
            playlist.songs.minByOrNull { abs(it.bpm - targetBpm) }
        } else {
            playlist.songs.firstOrNull()
        } ?: return

        p.addMediaItem(createMediaItem(startSong))
        p.prepare()
        p.play()
        
        // This will be called by transition listener too, but call manually for first 'next' item
        pickNextSongBasedOnTarget()
    }

    fun stopPlayback() {
        val p = _player.value ?: return
        p.stop()
        p.clearMediaItems()
        currentPlaylist = null
        playedHistory.clear()
    }

    fun stopAndSaveActivity(onSaved: (String) -> Unit) {
        val p = _player.value ?: return
        p.stop()
        p.clearMediaItems()

        viewModelScope.launch {
            val activityId = UUID.randomUUID().toString()
            val duration = if (_playedSongs.value.isNotEmpty()) _playedSongs.value.last().timestamp else 0L
            
            val activityEntity = ActivityEntity(
                id = activityId,
                name = _activityName.value.ifBlank { "Untitled Activity" },
                description = _activityDescription.value,
                playlistName = _playlistName.value,
                startTime = _startTimeMillis.value,
                duration = duration,
                mode = _activityMode.value
            )
            
            val playedSongEntities = _playedSongs.value.map { 
                PlayedSongEntity(
                    activityId = activityId,
                    title = it.title,
                    artist = it.artist,
                    timestamp = it.timestamp,
                    bpm = it.bpm,
                    cadence = it.cadence
                )
            }
            
            dao.insertActivity(activityEntity)
            dao.insertPlayedSongs(playedSongEntities)
            
            onSaved(activityId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
