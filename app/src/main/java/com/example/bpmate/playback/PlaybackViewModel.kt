package com.example.bpmate.playback

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.bpmate.data.PlayedSong
import com.example.bpmate.data.Playlist
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaybackViewModel : ViewModel() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _player = MutableStateFlow<Player?>(null)
    val player = _player.asStateFlow()

    private val _playedSongs = MutableStateFlow<List<PlayedSong>>(emptyList())
    val playedSongs = _playedSongs.asStateFlow()

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
                    
                    // Avoid duplicate entries for the exact same song at the exact same timestamp
                    val currentList = _playedSongs.value
                    if (currentList.lastOrNull()?.let { it.title == title && it.timestamp == timestamp } != true) {
                        _playedSongs.value = currentList + PlayedSong(title, artist, timestamp)
                    }
                }
            }
        })
    }

    fun setPlaylist(playlist: Playlist) {
        val player = _player.value ?: return
        
        // Reset state BEFORE triggering transitions
        activityStartTime = System.currentTimeMillis()
        _playedSongs.value = emptyList()

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

    fun stopPlayback() {
        val player = _player.value ?: return
        player.stop()
        player.clearMediaItems()
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
