package com.example.bpmate.ui.screens

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
                val (title, artist) = getSongMetadata(uri)
                // Use the new Audd.io function
                val bpm = getBpmFromAuddApi(title, artist)

                songEntities.add(
                    SongEntity(
                        id = UUID.randomUUID().toString(),
                        playlistId = playlistId,
                        title = title,
                        artist = artist,
                        uriString = uri.toString(),
                        bpm = bpm
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

    private fun getSongMetadata(uri: Uri): Pair<String, String> {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        try {
            retriever.setDataSource(getApplication(), uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        } catch (e: Exception) {
            Log.e("SONG_METADATA", "Error getting metadata for $uri", e)
        } finally {
            retriever.release()
        }

        val fileName = getFileName(uri)
        // Clean up filenames for better API matching
        val cleanedTitle = title ?: fileName.substringBeforeLast('.')
        return Pair(cleanedTitle, artist ?: "Unknown Artist")
    }

    private suspend fun getBpmFromAuddApi(title: String, artist: String): Int = withContext(Dispatchers.IO) {
        // TODO: Move API Token to a secure location like local.properties
        val apiToken = "009f83585b4ea54d5ea8aeb8f298ae70"

        try {
            // If the artist is unknown, search only by title. Otherwise, search by both.
            val query = if (artist == "Unknown Artist") {
                Log.d("API_CALL", "Artist is unknown. Searching by title only: '$title'")
                title
            } else {
                "$artist $title"
            }

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://api.audd.io/?method=findLyrics&q=$encodedQuery&return=song&api_token=$apiToken"

            Log.d("API_CALL", "Requesting Audd.io URL: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                Log.d("API_CALL", "Audd.io Response: $response")

                val jsonResponse = JSONObject(response)
                if (jsonResponse.optString("status") == "success") {
                    val resultArray = jsonResponse.optJSONArray("result")
                    if (resultArray != null && resultArray.length() > 0) {
                        val firstResult = resultArray.getJSONObject(0)
                        // The song object is the result itself, not nested.
                        val bpm = firstResult.optInt("bpm", 0)
                        if (bpm > 0) {
                            Log.d("API_CALL", "Success! Found BPM: $bpm")
                            return@withContext bpm
                        } else {
                            Log.w("API_CALL", "Audd.io found a match but it had no BPM.")
                        }
                    } else {
                        Log.w("API_CALL", "Audd.io returned success, but the result was empty.")
                    }
                } else {
                    Log.w("API_CALL", "Audd.io found no results for query '$query'. Status: ${jsonResponse.optString("status")}")
                }
            } else {
                Log.e("API_CALL", "Audd.io request failed with code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("API_CALL", "Error calling Audd.io API for query '$title'", e)
        }
        return@withContext 0
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
