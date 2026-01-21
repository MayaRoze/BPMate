package com.example.bpmate.data

import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val uri: Uri
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList()
)
