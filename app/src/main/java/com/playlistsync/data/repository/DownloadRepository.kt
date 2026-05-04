package com.playlistsync.data.repository

import android.content.Context
import com.playlistsync.data.model.PlaylistConfig
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistMetadata(
    val id: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val entries: List<VideoMetadata>
)

data class VideoMetadata(
    val ytId: String,
    val title: String,
    val duration: Long,
    val thumbnailUrl: String,
    val playlistIndex: Int
)

@Singleton
class DownloadRepository @Inject constructor() {

    suspend fun fetchPlaylistMetadata(url: String, proxyUrl: String = ""): PlaylistMetadata =
        withContext(Dispatchers.IO) {
            val request = YoutubeDLRequest(url).apply {
                addOption("--flat-playlist")
                addOption("--dump-single-json")
                addOption("--no-warnings")
                addOption("--socket-timeout", "30")
                addOption("--retries", "5")
                // Use Android + web clients to avoid login-required errors on public content
                addOption("--extractor-args", "youtube:player_client=android,web")
                if (proxyUrl.isNotBlank()) addOption("--proxy", proxyUrl)
            }
            val result = YoutubeDL.getInstance().execute(request, null, null)
            parsePlaylistJson(result.out, url)
        }

    private fun parsePlaylistJson(json: String, fallbackUrl: String): PlaylistMetadata {
        val root = JSONObject(json)
        val id = root.optString("id", extractPlaylistId(fallbackUrl))
        val title = root.optString("title", "Unknown Playlist")
        val channelName = root.optString("uploader", root.optString("channel", ""))
        val thumbnailUrl = root.optString("thumbnail", "")

        val entries = mutableListOf<VideoMetadata>()
        val entriesArray = root.optJSONArray("entries")
        if (entriesArray != null) {
            for (i in 0 until entriesArray.length()) {
                val entry = entriesArray.optJSONObject(i) ?: continue
                val ytId = entry.optString("id", "")
                if (ytId.isEmpty()) continue
                entries.add(
                    VideoMetadata(
                        ytId = ytId,
                        title = entry.optString("title", "Untitled"),
                        duration = entry.optLong("duration", 0L),
                        thumbnailUrl = entry.optString("thumbnail", ""),
                        playlistIndex = i
                    )
                )
            }
        }

        return PlaylistMetadata(
            id = id,
            title = title,
            channelName = channelName,
            thumbnailUrl = thumbnailUrl,
            entries = entries
        )
    }

    suspend fun downloadVideo(
        videoUrl: String,
        outputDir: File,
        config: PlaylistConfig,
        processId: String,
        onProgress: (Int, Long) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val formatString = buildFormatString(config)
        val outputTemplate = "${outputDir.absolutePath}/%(title)s [%(id)s].%(ext)s"

        val request = YoutubeDLRequest(videoUrl).apply {
            addOption("-f", formatString)
            addOption("-o", outputTemplate)
            addOption("--no-playlist")
            addOption("--no-warnings")
            addOption("--retries", "5")
            addOption("--fragment-retries", "5")
            addOption("--socket-timeout", "30")
            // Use Android + web clients to avoid login-required errors on public content
            addOption("--extractor-args", "youtube:player_client=android,web")

            if (config.proxyUrl.isNotBlank()) addOption("--proxy", config.proxyUrl)

            if (config.syncMode == "audio") {
                addOption("-x")
                addOption("--audio-format", config.audioFormat)
                addOption("--audio-quality", "0")
                if (config.embedThumbnail) addOption("--embed-thumbnail")
                addOption("--add-metadata")
            } else {
                addOption("--merge-output-format", config.videoContainer)
                if (config.embedSubs) {
                    addOption("--embed-subs")
                    addOption("--sub-langs", "en")
                }
            }

            if (config.useAria2c) {
                addOption("--downloader", "aria2c")
                addOption("--downloader-args", "aria2c:-x16 -s16 -k1M")
            }

            if (config.extraArgs.isNotBlank()) {
                config.extraArgs.trim().split("\\s+".toRegex()).forEach { arg ->
                    addOption(arg)
                }
            }
        }

        YoutubeDL.getInstance().execute(
            request = request,
            processId = processId,
            callback = { progress, etaSeconds, _ ->
                onProgress(progress.toInt(), etaSeconds.toLong())
            }
        )

        outputDir.listFiles()
            ?.filter { it.isFile }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath ?: ""
    }

    fun cancelDownload(processId: String) {
        YoutubeDL.getInstance().destroyProcessById(processId)
    }

    suspend fun updateYtDlp(context: Context): YoutubeDL.UpdateStatus =
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                ?: YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE
        }

    private fun buildFormatString(config: PlaylistConfig): String {
        if (config.formatString.isNotBlank()) return config.formatString
        return if (config.syncMode == "audio") {
            "bestaudio[ext=m4a]/bestaudio"
        } else {
            when (config.qualityPreset) {
                "1080" -> "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/best[height<=1080]"
                "720"  -> "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[height<=720]"
                "480"  -> "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/best[height<=480]"
                else   -> "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
            }
        }
    }

    private fun extractPlaylistId(url: String): String =
        Regex("[?&]list=([A-Za-z0-9_-]+)").find(url)?.groupValues?.get(1)
            ?: url.hashCode().toString()
}
