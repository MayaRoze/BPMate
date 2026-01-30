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

    fun connectController(context: Context) {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val p = controllerFuture?.get()
            _player.value = p
            setupMediaListener(p)
        }, MoreExecutors.directExecutor())
    }

    private fun setupMediaListener(player: Player?) {
        player?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // When a song starts (either auto or manual), we immediately prepare the NEXT one
                pickNextSongBasedOnCadence()

                mediaItem?.mediaMetadata?.let { metadata ->
                    val title = metadata.title?.toString() ?: "Unknown"
                    val artist = metadata.artist?.toString() ?: "Unknown Artist"
                    val timestamp = if (activityStartTime == 0L) 0L else System.currentTimeMillis() - activityStartTime
                    
                    val currentList = _playedSongs.value
                    if (currentList.lastOrNull()?.let { it.title == title && it.timestamp == timestamp } != true) {
                        _playedSongs.value = currentList + PlayedSong(title, artist, timestamp)
                    }
                }
            }
        })
    }

    fun updateCadence(cadence: Float) {
        // If cadence changes significantly (more than 5 steps/min), re-pick the next song
        if (abs(currentCadence - cadence) > 5f) {
            currentCadence = cadence
            pickNextSongBasedOnCadence()
        } else {
            currentCadence = cadence
        }
    }

    fun skipToBestMatch() {
        val p = _player.value ?: return
        val playlist = currentPlaylist ?: return
        if (playlist.songs.isEmpty()) return

        val targetBpm = if (currentCadence > 0) currentCadence.toInt() else return
        
        // Find best song excluding current if possible
        val currentMediaId = p.currentMediaItem?.mediaId
        val bestSong = playlist.songs
            .filter { it.id != currentMediaId }
            .minByOrNull { abs(it.bpm - targetBpm) } 
            ?: playlist.songs.minByOrNull { abs(it.bpm - targetBpm) } ?: return

        val index = playlist.songs.indexOf(bestSong)
        if (index != -1) {
            p.seekTo(index, 0L)
            Log.d("PlaybackViewModel", "Manually skipped to best match: ${bestSong.title} (BPM: ${bestSong.bpm})")
        }
    }

    private fun pickNextSongBasedOnCadence() {
        val p = _player.value ?: return
        val playlist = currentPlaylist ?: return
        if (playlist.songs.isEmpty()) return

        val targetBpm = currentCadence.toInt()
        if (targetBpm <= 0) return

        // Pick next song that is NOT the current one to be queued
        val currentMediaId = p.currentMediaItem?.mediaId
        val nextSong = playlist.songs
            .filter { it.id != currentMediaId }
            .minByOrNull { abs(it.bpm - targetBpm) } ?: return
        
        val nextIndex = p.nextMediaItemIndex
        if (nextIndex != C.INDEX_UNSET) {
            val mediaItem = createMediaItem(nextSong)
            p.replaceMediaItem(nextIndex, mediaItem)
            Log.d("PlaybackViewModel", "Dynamically queued next: ${nextSong.title} (BPM: ${nextSong.bpm}) for cadence: $targetBpm")
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
        val player = _player.value ?: return
        currentPlaylist = playlist
        activityStartTime = System.currentTimeMillis()
        _playedSongs.value = emptyList()
        
        if (_playlistName.value.isBlank()) {
            _playlistName.value = playlist.name
            _startTimeMillis.value = System.currentTimeMillis()
        }

        val mediaItems = playlist.songs.map { createMediaItem(it) }
        player.setMediaItems(mediaItems)
        player.shuffleModeEnabled = false 
        player.prepare()
        player.play()
        
        // Seek to best starting song based on current cadence
        if (currentCadence > 0) {
            val targetBpm = currentCadence.toInt()
            val bestStartIndex = playlist.songs.indexOf(playlist.songs.minByOrNull { abs(it.bpm - targetBpm) })
            if (bestStartIndex != -1) {
                player.seekTo(bestStartIndex, 0L)
            }
        }
        
        // Ensure the NEXT one in queue is also set optimally
        pickNextSongBasedOnCadence()
    }

    fun stopAndSaveActivity(onSaved: (String) -> Unit) {
        val player = _player.value ?: return
        player.stop()
        player.clearMediaItems()

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
                    timestamp = it.timestamp
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
