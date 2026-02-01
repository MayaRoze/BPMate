package com.example.bpmate.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume

object BpmAnalyzer {

    data class SongInfo(val title: String, val artist: String, val bpm: Int)

    /**
     * Extracts song metadata and infers BPM.
     * Prioritizes the "Beats-per-minute" (TBPM) ID3 tag found in MP3 files using Media3.
     */
    suspend fun getSongInfo(context: Context, uri: Uri): SongInfo = withContext(Dispatchers.IO) {
        val (title, artist) = getSongMetadata(context, uri)
        val fileName = getFileName(context, uri)

        // 1. Try to extract BPM from ID3 "TBPM" or "TBP" tag using Media3
        var inferredBpm = extractBpmFromId3(context, uri)

        // 2. Fallback: Try to extract BPM from filename (e.g. "Song Name [120].mp3")
        if (inferredBpm == null) {
            inferredBpm = extractBpmFromString(fileName)
        }

        // 3. Fallback: Try to extract BPM from Title metadata (e.g. "Song Name 128BPM")
        if (inferredBpm == null) {
            inferredBpm = extractBpmFromString(title)
        }

        // Return inferred BPM or default to 120
        return@withContext SongInfo(title, artist, inferredBpm ?: 120)
    }

    /**
     * Uses Media3's MetadataRetriever to look for TBPM or TBP ID3 frames.
     */
    @OptIn(UnstableApi::class)
    private suspend fun extractBpmFromId3(context: Context, uri: Uri): Int? = suspendCancellableCoroutine { continuation ->
        try {
            val mediaItem = MediaItem.fromUri(uri)
            val future = MetadataRetriever.retrieveMetadata(context, mediaItem)
            
            future.addListener({
                try {
                    val trackGroups = future.get()
                    var bpm: Int? = null
                    
                    for (i in 0 until trackGroups.length) {
                        val trackGroup = trackGroups.get(i)
                        for (j in 0 until trackGroup.length) {
                            val metadata = trackGroup.getFormat(j).metadata ?: continue
                            for (k in 0 until metadata.length()) {
                                val entry = metadata.get(k)
                                if (entry is TextInformationFrame) {
                                    // TBPM is the standard ID3v2 tag for Beats Per Minute
                                    if (entry.id == "TBPM" || entry.id == "TBP") {
                                        bpm = entry.value.toIntOrNull()
                                        if (bpm != null) break
                                    }
                                }
                            }
                            if (bpm != null) break
                        }
                        if (bpm != null) break
                    }
                    continuation.resume(bpm)
                } catch (e: ExecutionException) {
                    continuation.resume(null)
                } catch (e: InterruptedException) {
                    continuation.resume(null)
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    /**
     * Looks for a number inside brackets [120] or followed by "BPM".
     */
    private fun extractBpmFromString(text: String): Int? {
        // Match [120]
        val bracketMatch = "\\[(\\d+)\\]".toRegex().find(text)
        if (bracketMatch != null) return bracketMatch.groupValues[1].toIntOrNull()

        // Match "120 BPM" or "120BPM" (case insensitive)
        val bpmMatch = "(\\d+)\\s*(?i)BPM".toRegex().find(text)
        if (bpmMatch != null) return bpmMatch.groupValues[1].toIntOrNull()

        return null
    }

    private fun getSongMetadata(context: Context, uri: Uri): Pair<String, String> {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        try {
            if (uri.scheme == "asset") {
                val path = uri.path?.removePrefix("/") ?: ""
                val afd = context.assets.openFd(path)
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            } else {
                retriever.setDataSource(context, uri)
            }
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        } catch (e: Exception) {
            Log.e("BpmAnalyzer", "Metadata error for $uri: ${e.message}")
        } finally {
            retriever.release()
        }

        val fileName = getFileName(context, uri)
        val cleanedTitle = title ?: fileName.substringBeforeLast('.')
        return Pair(cleanedTitle, artist ?: "Unknown Artist")
    }

    private fun getFileName(context: Context, uri: Uri): String {
        if (uri.scheme == "asset") {
            return uri.lastPathSegment ?: "Unknown"
        }
        var name = "Unknown Track"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            name = uri.lastPathSegment ?: "Unknown"
        }
        return name
    }
}
