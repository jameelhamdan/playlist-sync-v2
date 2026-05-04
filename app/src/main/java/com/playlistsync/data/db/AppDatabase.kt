package com.playlistsync.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.playlistsync.data.model.PlaylistEntity
import com.playlistsync.data.model.VideoEntity

@Database(
    entities = [PlaylistEntity::class, VideoEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun videoDao(): VideoDao
}
