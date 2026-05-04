package com.playlistsync.data.db

import androidx.room.*
import com.playlistsync.data.model.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class VideoDao {
    @Query("SELECT * FROM videos WHERE playlistId = :playlistId ORDER BY playlistIndex ASC")
    abstract fun observeByPlaylist(playlistId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE playlistId = :playlistId AND status IN ('pending', 'error') ORDER BY playlistIndex ASC")
    abstract suspend fun getPendingOrErrorByPlaylist(playlistId: String): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE playlistId = :playlistId AND status IN ('pending', 'error') ORDER BY playlistIndex ASC LIMIT 1")
    abstract suspend fun getFirstPendingOrError(playlistId: String): VideoEntity?

    // Atomically claim a video for download — only marks it if still pending/error, preventing
    // two concurrent workers from picking the same video.
    @Transaction
    open suspend fun claimNextPendingVideo(playlistId: String): VideoEntity? {
        val video = getFirstPendingOrError(playlistId) ?: return null
        markAsDownloading(video.id)
        return video
    }

    @Query("UPDATE videos SET status = 'downloading', downloadProgress = 0, errorMessage = NULL WHERE id = :id AND status IN ('pending', 'error')")
    abstract suspend fun markAsDownloading(id: String)

    @Query("SELECT COUNT(*) FROM videos WHERE playlistId = :playlistId AND status IN ('pending', 'error')")
    abstract suspend fun countPendingOrError(playlistId: String): Int

    @Query("SELECT ytId FROM videos WHERE playlistId = :playlistId")
    abstract suspend fun getKnownYtIds(playlistId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAllIfAbsent(videos: List<VideoEntity>)

    @Query("""
        UPDATE videos SET status = :status, downloadProgress = :progress,
        localPath = :localPath, downloadedAt = :downloadedAt, errorMessage = :errorMessage
        WHERE id = :id
    """)
    abstract suspend fun updateDownloadState(
        id: String,
        status: String,
        progress: Int,
        localPath: String?,
        downloadedAt: Long?,
        errorMessage: String?
    )

    @Query("DELETE FROM videos WHERE playlistId = :playlistId")
    abstract suspend fun deleteByPlaylist(playlistId: String)

    @Query("SELECT COUNT(*) FROM videos WHERE playlistId = :playlistId AND status = 'downloaded'")
    abstract suspend fun countDownloaded(playlistId: String): Int
}
