package com.playlistsync.data.model

import androidx.room.ColumnInfo

data class PlaylistConfig(
    @ColumnInfo(name = "sync_mode")
    val syncMode: String = "audio",

    @ColumnInfo(name = "format_string")
    val formatString: String = "",

    @ColumnInfo(name = "audio_format")
    val audioFormat: String = "m4a",

    @ColumnInfo(name = "video_container")
    val videoContainer: String = "mp4",

    @ColumnInfo(name = "quality_preset")
    val qualityPreset: String = "best",

    @ColumnInfo(name = "extra_args")
    val extraArgs: String = "",

    @ColumnInfo(name = "embed_thumbnail")
    val embedThumbnail: Boolean = true,

    @ColumnInfo(name = "embed_subs")
    val embedSubs: Boolean = false,

    @ColumnInfo(name = "use_aria2c")
    val useAria2c: Boolean = false,

    @ColumnInfo(name = "proxy_url")
    val proxyUrl: String = "",

    @ColumnInfo(name = "max_concurrent_downloads")
    val maxConcurrentDownloads: Int = 2
)
