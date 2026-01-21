package com.example.bpmate.ui.screens

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpmate.data.Playlist
import com.example.bpmate.data.Song
import com.example.bpmate.data.local.AppDatabase
import com.example.bpmate.data.local.PlaylistEntity
import com.example.bpmate.data.local.SongEntity
import com.example.bpmate.data.local.ActivityWithPlayedSongs
import com.example.bpmate.data.local.ActivityEntity
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
                        Song(song.id, song.title, song.artist, Uri.parse(song.uriString))
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
        viewModelScope.launch {
            val songEntities = uris.map { uri ->
                val fileName = getFileName(uri)
                SongEntity(
                    id = UUID.randomUUID().toString(),
                    playlistId = playlistId,
                    title = fileName,
                    artist = "Local File",
                    uriString = uri.toString()
                )
            }
            dao.insertSongs(songEntities)
        }
    }

    fun removeSong(songId: String, playlistId: String, title: String, artist: String, uriString: String) {
        viewModelScope.launch {
            dao.deleteSong(SongEntity(songId, playlistId, title, artist, uriString))
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "Unknown Track"
        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
}
