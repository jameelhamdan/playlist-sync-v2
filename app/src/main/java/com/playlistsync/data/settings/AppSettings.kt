package com.playlistsync.data.settings

data class AppSettings(
    val syncIntervalHours: Int = 6,               // 1 / 2 / 6 / 12 / 24
    val wifiOnly: Boolean = false,
    val requireBatteryNotLow: Boolean = true,
    val defaultSyncMode: String = "audio",         // "audio" | "video"
    val defaultAudioFormat: String = "m4a",        // m4a / mp3 / opus / flac / wav
    val defaultVideoQuality: String = "best",      // best / 1080p / 720p / 480p / 360p
    val defaultEmbedThumbnail: Boolean = true,
    val defaultConcurrentDownloads: Int = 2,       // 1–5
    val defaultProxyUrl: String = ""
)
