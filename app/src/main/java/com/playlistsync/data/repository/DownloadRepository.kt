package com.playlistsync.data.repository

import android.content.Context
import com.playlistsync.data.model.PlaylistConfig
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                addOption("--extractor-args", "youtube:player_client=ios,web")
                if (proxyUrl.isNotBlank()) addOption("--proxy", proxyUrl)
            }
            val result = YoutubeDL.getInstance().execute(request, null, null)
            parsePlaylistJson(result.out, url)
        }

    suspend fun downloadVideo(
        videoUrl: String,
        outputDir: File,
        config: PlaylistConfig,
        processId: String,
        playlistName: String = "",
        channelName: String = "",
        trackNumber: Int = 0,
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
            addOption("--extractor-args", "youtube:player_client=ios,web")

            if (config.proxyUrl.isNotBlank()) addOption("--proxy", config.proxyUrl)

            if (config.syncMode == "audio") {
                addOption("-x")
                addOption("--audio-format", config.audioFormat)
                addOption("--audio-quality", "0")
                // m4a/opus/flac/wav all support thumbnail embedding via mutagen (no AtomicParsley needed)
                if (config.embedThumbnail && config.audioFormat in listOf("m4a", "opus", "flac", "wav")) {
                    addOption("--embed-thumbnail")
                }
                addOption("--add-metadata")
                // Map yt-dlp's uploader field → artist tag (--add-metadata does title/date but
                // doesn't always populate artist/album when downloading individual video URLs)
                addOption("--parse-metadata", "%(uploader)s:%(meta_artist)s")
                // Set album, album_artist, and track via ffmpeg postprocessor args
                buildAudioMetadataArgs(channelName, playlistName, trackNumber)?.let { args ->
                    addOption("--postprocessor-args", args)
                }
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

    companion object {
        internal data class DownloadArgs(
            val formatString: String,
            val extractAudio: Boolean,
            val audioFormat: String,
            val audioQuality: String,
            val embedThumbnail: Boolean,
            val addMetadata: Boolean,
            val metadataPostprocessorArgs: String?,
            val mergeOutputFormat: String,
            val embedSubs: Boolean,
            val useAria2c: Boolean,
            val extraArgs: List<String>
        )

        internal fun buildDownloadArgs(
            config: PlaylistConfig,
            channelName: String = "",
            playlistName: String = "",
            trackNumber: Int = 0
        ): DownloadArgs {
            val isAudio = config.syncMode == "audio"
            return DownloadArgs(
                formatString = buildFormatString(config),
                extractAudio = isAudio,
                audioFormat = config.audioFormat,
                audioQuality = "0",
                embedThumbnail = isAudio && config.embedThumbnail &&
                    config.audioFormat in listOf("m4a", "opus", "flac", "wav"),
                addMetadata = isAudio,
                metadataPostprocessorArgs = if (isAudio)
                    buildAudioMetadataArgs(channelName, playlistName, trackNumber)
                else null,
                mergeOutputFormat = config.videoContainer,
                embedSubs = !isAudio && config.embedSubs,
                useAria2c = config.useAria2c,
                extraArgs = if (config.extraArgs.isBlank()) emptyList()
                    else config.extraArgs.trim().split("\\s+".toRegex())
            )
        }
        // yt-dlp parses --postprocessor-args with shlex, so values with spaces need single-quoting.
        // We strip single-quotes from names to avoid escaping complexity.
        internal fun buildAudioMetadataArgs(channelName: String, playlistName: String, trackNumber: Int): String? {
            val parts = mutableListOf<String>()
            if (channelName.isNotBlank()) {
                val safe = channelName.replace("'", "")
                parts += "-metadata 'album_artist=$safe'"
            }
            if (playlistName.isNotBlank()) {
                val safe = playlistName.replace("'", "")
                parts += "-metadata 'album=$safe'"
            }
            if (trackNumber > 0) parts += "-metadata track=$trackNumber"
            return if (parts.isEmpty()) null else "ffmpeg:${parts.joinToString(" ")}"
        }

        internal fun buildFormatString(config: PlaylistConfig): String {
            if (config.formatString.isNotBlank()) return config.formatString
            return if (config.syncMode == "audio") {
                // No ext restriction — let -x + --audio-format convert to the desired codec.
                // Restricting to [ext=m4a] fails when YouTube only serves opus/webm streams.
                "bestaudio/best"
            } else {
                val h = config.qualityPreset.removeSuffix("p")
                when (h) {
                    "1080" -> "bestvideo[height<=1080]+bestaudio/bestvideo[height<=1080]/best[height<=1080]/best"
                    "720"  -> "bestvideo[height<=720]+bestaudio/bestvideo[height<=720]/best[height<=720]/best"
                    "480"  -> "bestvideo[height<=480]+bestaudio/bestvideo[height<=480]/best[height<=480]/best"
                    "360"  -> "bestvideo[height<=360]+bestaudio/bestvideo[height<=360]/best[height<=360]/best"
                    else   -> "bestvideo+bestaudio/best"
                }
            }
        }

        internal fun extractPlaylistId(url: String): String =
            Regex("[?&]list=([A-Za-z0-9_-]+)").find(url)?.groupValues?.get(1)
                ?: url.hashCode().toString()

        internal fun parsePlaylistJson(json: String, fallbackUrl: String): PlaylistMetadata {
            val root = org.json.JSONObject(json)
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
    }
}
