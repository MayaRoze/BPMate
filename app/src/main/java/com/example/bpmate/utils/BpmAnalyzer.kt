package com.example.bpmate.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object BpmAnalyzer {

    // IMPORTANT: Ensure your key is active by having a backlink to getsongbpm.com in your app.
    private const val API_KEY = "API_KEY_HERE"

    data class SongInfo(val title: String, val artist: String, val bpm: Int)

    /**
     * Searches for song metadata and BPM using the GetSongBPM (GetSong.co) API.
     */
    suspend fun getSongInfo(context: Context, uri: Uri): SongInfo = withContext(Dispatchers.IO) {
        val (title, artist) = getSongMetadata(context, uri)

        // Clean metadata for better search accuracy
        val cleanTitle = title.split("(")[0].split("-")[0].replace(Regex("(?i)feat.*"), "").trim()
        val cleanArtist = if (artist != "Unknown Artist") artist.split(",")[0].split("&")[0].trim() else ""

        var bpm = 0
        try {
            val query = if (cleanArtist.isEmpty()) cleanTitle else "$cleanArtist $cleanTitle"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            val urlString = "https://api.getsong.co/search/?type=song&lookup=$encodedQuery"
            Log.d("BpmAnalyzer", "Requesting: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (BPMate Android App)")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-API-KEY", API_KEY)
            connection.setRequestProperty("Referer", "https://getsongbpm.com")

            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("BpmAnalyzer", "Raw JSON: $response")

                val jsonResponse = JSONObject(response)
                val searchResult = jsonResponse.optJSONArray("search") ?: jsonResponse.optJSONArray("search_results")

                if (searchResult != null && searchResult.length() > 0) {
                    val song = searchResult.getJSONObject(0)
                    val tempoObj = song.opt("tempo")
                    bpm = when (tempoObj) {
                        is Number -> tempoObj.toInt()
                        is String -> tempoObj.split(".")[0].toIntOrNull() ?: 0
                        else -> 0
                    }
                    Log.d("BpmAnalyzer", "Match found! BPM: $bpm")
                }
            } else {
                Log.e("BpmAnalyzer", "HTTP Error: $responseCode. Ensure you have a backlink to getsongbpm.com in your app.")
            }
        } catch (e: Exception) {
            Log.e("BpmAnalyzer", "Exception during API call: ${e.message}")
        }

        return@withContext SongInfo(title, artist, if (bpm > 0) bpm else 120)
    }

    private fun getSongMetadata(context: Context, uri: Uri): Pair<String, String> {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        } catch (e: Exception) {
            Log.e("SONG_METADATA", "Error reading metadata for $uri", e)
        } finally {
            retriever.release()
        }

        val fileName = getFileName(context, uri)
        val cleanedTitle = title ?: fileName.substringBeforeLast('.')
        return Pair(cleanedTitle, artist ?: "Unknown Artist")
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "Unknown Track"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
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