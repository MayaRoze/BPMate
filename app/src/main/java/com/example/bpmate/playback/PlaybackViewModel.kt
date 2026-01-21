package com.example.bpmate.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import android.content.Context
import com.example.bpmate.data.PlayedSong
import com.example.bpmate.data.Playlist
import com.example.bpmate.data.local.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

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
        activityStartTime = System.currentTimeMillis()
        _playedSongs.value = emptyList()
        
        if (_playlistName.value.isBlank()) {
            _playlistName.value = playlist.name
            _startTimeMillis.value = System.currentTimeMillis()
        }

        val mediaItems = playlist.songs.map { song ->
            MediaItem.Builder()
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
        player.setMediaItems(mediaItems)
        player.shuffleModeEnabled = true
        player.prepare()
        player.play()
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
