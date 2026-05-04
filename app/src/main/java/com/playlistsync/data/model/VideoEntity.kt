package com.playlistsync.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("ytId")]
)
data class VideoEntity(
    @PrimaryKey
    val id: String,

    val playlistId: String,
    val ytId: String,
    val title: String,
    val duration: Long = 0L,
    val thumbnailUrl: String = "",
    val playlistIndex: Int = 0,

    val status: String = "pending",

    val localPath: String? = null,
    val mimeType: String? = null,
    val downloadProgress: Int = 0,
    val errorMessage: String? = null,

    val addedAt: Long = System.currentTimeMillis(),
    val downloadedAt: Long? = null
)
