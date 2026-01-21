package com.example.bpmate.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(
    tableName = "songs",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val title: String,
    val artist: String,
    val uriString: String
)

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val songs: List<SongEntity>
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val playlistName: String,
    val startTime: Long,
    val duration: Long,
    val mode: String
)

@Entity(
    tableName = "played_songs",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlayedSongEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Int = 0,
    val activityId: String,
    val title: String,
    val artist: String,
    val timestamp: Long
)

data class ActivityWithPlayedSongs(
    @Embedded val activity: ActivityEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "activityId"
    )
    val playedSongs: List<PlayedSongEntity>
)
