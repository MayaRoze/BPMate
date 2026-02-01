package com.example.bpmate.ui.screens

import android.app.Application
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.flow.first
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

    init {
        viewModelScope.launch {
            syncAssetPlaylists()
        }
    }

    private suspend fun syncAssetPlaylists() {
        val assetManager = getApplication<Application>().assets
        val playlistsRoot = "Playlists"
        
        // 1. Get current folders in assets
        val assetFolders = try { 
            assetManager.list(playlistsRoot)?.toSet() ?: emptySet() 
        } catch (e: Exception) { 
            emptySet() 
        }

        val existingPlaylists = dao.getPlaylistsWithSongs().first()

        // 2. Refresh or create playlists based on asset folders
        for (folderName in assetFolders) {
            val folderPath = "$playlistsRoot/$folderName"
            val songFiles = assetManager.list(folderPath) ?: continue
            val audioFiles = songFiles.filter { isAudioFile(it) }
            
            // Delete existing version to handle file changes/deletions within the folder
            existingPlaylists.find { it.playlist.name == folderName }?.let {
                dao.deletePlaylist(it.playlist)
            }

            if (audioFiles.isEmpty()) continue

            val playlistId = UUID.randomUUID().toString()
            dao.insertPlaylist(PlaylistEntity(id = playlistId, name = folderName))

            val songEntities = mutableListOf<SongEntity>()
            for (songFile in audioFiles) {
                val assetUri = Uri.parse("asset:///$folderPath/$songFile")
                try {
                    val songInfo = BpmAnalyzer.getSongInfo(getApplication(), assetUri)
                    songEntities.add(SongEntity(
                        id = UUID.randomUUID().toString(),
                        playlistId = playlistId,
                        title = songInfo.title,
                        artist = songInfo.artist,
                        uriString = assetUri.toString(),
                        bpm = songInfo.bpm
                    ))
                } catch (e: Exception) {
                    Log.e("Sync", "Failed to load $songFile", e)
                }
            }
            dao.insertSongs(songEntities)
        }

        // 3. Remove playlists that are no longer in assets (but were asset-managed)
        // We assume any playlist created via assets won't be manually created with the same name.
        // For robustness, we could track 'isAssetManaged' in the DB, but name-check is usually enough.
    }

    private fun isAudioFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in setOf("mp3", "m4a", "wav", "ogg", "aac")
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            dao.insertPlaylist(PlaylistEntity(id = UUID.randomUUID().toString(), name = name))
        }
    }

    fun deletePlaylist(playlistId: String, name: String) {
        viewModelScope.launch {
            // Prevent deletion of asset-based default playlists from UI
            val assetFolders = getApplication<Application>().assets.list("Playlists") ?: emptyArray()
            if (name in assetFolders) return@launch
            
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

    fun updateSongBpm(songId: String, playlistId: String, title: String, artist: String, uriString: String, newBpm: Int) {
        viewModelScope.launch {
            dao.updateSong(SongEntity(songId, playlistId, title, artist, uriString, newBpm))
        }
    }

    fun removeSong(songId: String, playlistId: String, title: String, artist: String, uriString: String, bpm: Int) {
        viewModelScope.launch {
            dao.deleteSong(SongEntity(songId, playlistId, title, artist, uriString, bpm))
        }
    }
}
