package com.playlistsync.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,

    val url: String,
    val name: String,
    val thumbnailUrl: String = "",
    val channelName: String = "",
    val videoCount: Int = 0,
    val downloadedCount: Int = 0,

    val lastSyncedAt: Long? = null,
    val autoSyncEnabled: Boolean = true,

    @Embedded
    val config: PlaylistConfig = PlaylistConfig()
)
