package com.example.bpmate.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpmate.data.Playlist
import com.example.bpmate.data.Song
import com.example.bpmate.data.local.ActivityEntity
import com.example.bpmate.data.local.ActivityWithPlayedSongs
import com.example.bpmate.data.local.AppDatabase
import com.example.bpmate.data.local.PlaylistEntity
import com.example.bpmate.data.local.SongEntity
import com.example.bpmate.utils.BpmAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).playlistDao()

    val playlists: StateFlow<List<Playlist>> = dao.getPlaylistsWithSongs()
        .map { list ->
            list.map { item ->
                Playlist(
                    id = item.playlist.id,
                    name = item.playlist.name,
                    songs = item.songs.map { song ->
                        Song(song.id, song.title, song.artist, Uri.parse(song.uriString), song.bpm)
                    }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activityHistory: StateFlow<List<ActivityWithPlayedSongs>> = dao.getAllActivities()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            dao.insertPlaylist(PlaylistEntity(id = UUID.randomUUID().toString(), name = name))
        }
    }

    fun deletePlaylist(playlistId: String, name: String) {
        viewModelScope.launch {
            dao.deletePlaylist(PlaylistEntity(playlistId, name))
        }
    }

    fun deleteActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            dao.deleteActivity(activity)
        }
    }

    fun addSongsToPlaylist(playlistId: String, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val songEntities = mutableListOf<SongEntity>()
            for (uri in uris) {
                val songInfo = BpmAnalyzer.getSongInfo(getApplication(), uri)

                songEntities.add(
                    SongEntity(
                        id = UUID.randomUUID().toString(),
                        playlistId = playlistId,
                        title = songInfo.title,
                        artist = songInfo.artist,
                        uriString = uri.toString(),
                        bpm = songInfo.bpm
                    )
                )
            }
            if (songEntities.isNotEmpty()) {
                dao.insertSongs(songEntities)
            }
        }
    }

    fun removeSong(songId: String, playlistId: String, title: String, artist: String, uriString: String, bpm: Int) {
        viewModelScope.launch {
            dao.deleteSong(SongEntity(songId, playlistId, title, artist, uriString, bpm))
        }
    }
}
