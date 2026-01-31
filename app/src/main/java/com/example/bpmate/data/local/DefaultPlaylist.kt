package com.example.bpmate.data.local

import android.net.Uri
import com.example.bpmate.R
import com.example.bpmate.data.Song
import java.util.UUID

object DefaultPlaylist {
    const val PLAYLIST_NAME = "BPMate Favorites"

    fun getSongs(): List<Song> {
        return listOf(
            // --- 70-90 BPM (Slow / Casual Stroll) ---
            Song(
                id = UUID.randomUUID().toString(),
                title = "Let It Be",
                artist = "The Beatles",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.let_it_be),
                bpm = 71
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Hey Jude",
                artist = "The Beatles",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.hey_jude),
                bpm = 74
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Every Breath You Take",
                artist = "The Police",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.every_breath_you_take),
                bpm = 84
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Yesterday",
                artist = "The Beatles",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.yesterday),
                bpm = 97
            ),

            // --- 100-120 BPM (Brisk / Fitness Walk) ---
            Song(
                id = UUID.randomUUID().toString(),
                title = "Man in the Mirror",
                artist = "Michael Jackson",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.man_in_the_mirror),
                bpm = 100
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Another One Bites the Dust",
                artist = "Queen",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.another_one_bites_the_dust),
                bpm = 110
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "נתתי לה חיי",
                artist = "כוורת",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.i_gave_her_my_life),
                bpm = 110
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Billie Jean",
                artist = "Michael Jackson",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.billie_jean),
                bpm = 117
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Start Me Up",
                artist = "The Rolling Stones",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.start_me_up),
                bpm = 122
            ),

            // --- 120-140 BPM (Power Walk / Fast Tempo) ---
            Song(
                id = UUID.randomUUID().toString(),
                title = "Come Together",
                artist = "The Beatles",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.come_together),
                bpm = 165
            ),

            Song(
                id = UUID.randomUUID().toString(),
                title = "Thriller",
                artist = "Michael Jackson",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.thriller),
                bpm = 118
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Living On A Prayer",
                artist = "Bon Jovi",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.living_on_a_prayer),
                bpm = 123
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Smooth Criminal",
                artist = "Michael Jackson",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.smooth_criminal),
                bpm = 118
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Beat It",
                artist = "Michael Jackson",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.beat_it),
                bpm = 139
            ),

            // Running
            Song(
                id = UUID.randomUUID().toString(),
                title = "We Didn't Start The Fire",
                artist = "The Beatles",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.we_didnt_start_the_fire),
                bpm = 191
            ),
            Song(
                id = UUID.randomUUID().toString(),
                title = "Help!",
                artist = "The Beatles",
                uri = Uri.parse("android.resource://com.example.bpmate/" + R.raw.help),
                bpm = 191
            )

        ).sortedBy { it.bpm }
    }
}
