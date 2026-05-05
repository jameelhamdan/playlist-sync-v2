package com.playlistsync.data.repository

import com.playlistsync.data.db.PlaylistDao
import com.playlistsync.data.db.VideoDao
import com.playlistsync.data.model.PlaylistConfig
import com.playlistsync.data.model.PlaylistEntity
import com.playlistsync.data.model.VideoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val videoDao: VideoDao
) {
    fun observeAll(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()

    suspend fun getById(id: String): PlaylistEntity? = playlistDao.getById(id)

    suspend fun save(playlist: PlaylistEntity) = playlistDao.upsert(playlist)

    suspend fun update(playlist: PlaylistEntity) = playlistDao.update(playlist)

    suspend fun delete(playlist: PlaylistEntity) {
        videoDao.deleteByPlaylist(playlist.id)
        playlistDao.delete(playlist)
    }

    suspend fun updateConfig(playlistId: String, config: PlaylistConfig) {
        playlistDao.getById(playlistId)?.let {
            playlistDao.update(it.copy(config = config))
        }
    }

    fun observeVideos(playlistId: String) = videoDao.observeByPlaylist(playlistId)

    suspend fun insertVideos(videos: List<VideoEntity>) = videoDao.insertAllIfAbsent(videos)

    suspend fun getKnownYtIds(playlistId: String): List<String> =
        videoDao.getKnownYtIds(playlistId)

    suspend fun updateVideoDownloadState(
        id: String,
        status: String,
        progress: Int = 0,
        localPath: String? = null,
        downloadedAt: Long? = null,
        errorMessage: String? = null
    ) = videoDao.updateDownloadState(id, status, progress, localPath, downloadedAt, errorMessage)

    suspend fun countDownloaded(playlistId: String) = videoDao.countDownloaded(playlistId)

    suspend fun getAllAutoSync(): List<PlaylistEntity> = playlistDao.getAllAutoSync()

    suspend fun updateSyncState(playlistId: String, timestamp: Long, count: Int) =
        playlistDao.updateSyncState(playlistId, timestamp, count)

    suspend fun getPendingOrErrorVideos(playlistId: String) =
        videoDao.getPendingOrErrorByPlaylist(playlistId)

    suspend fun claimNextPendingVideo(playlistId: String) =
        videoDao.claimNextPendingVideo(playlistId)

    suspend fun hasPendingVideos(playlistId: String) =
        videoDao.countPendingOrError(playlistId) > 0

    suspend fun hasOnlyPendingVideos(playlistId: String) =
        videoDao.countPendingOnly(playlistId) > 0
}
