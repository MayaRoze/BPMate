package com.example.bpmate.data

import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val uri: Uri,
    val bpm: Int
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList()
)

data class PlayedSong(
    val title: String,
    val artist: String,
    val timestamp: Long, // Relative to activity start in ms
    val bpm: Int = 0,
    val cadence: Int = 0
)

data class ActivitySession(
    val id: String,
    val playlistId: String,
    val startTime: Long,
    val playedSongs: List<PlayedSong>
)
