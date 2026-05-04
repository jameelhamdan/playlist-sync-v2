package com.playlistsync.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.playlistsync.R
import com.playlistsync.data.repository.DownloadRepository
import com.playlistsync.data.repository.PlaylistRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val playlistRepository: PlaylistRepository,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        buildForegroundInfo("Preparing download…", 0)

    override suspend fun doWork(): Result {
        val playlistId = inputData.getString(KEY_PLAYLIST_ID) ?: return Result.failure()
        val slot = inputData.getInt(KEY_SLOT, 0)
        val playlist = playlistRepository.getById(playlistId) ?: return Result.failure()

        val video = playlistRepository.claimNextPendingVideo(playlistId)
            ?: return Result.success() // No work left for this slot

        setForeground(buildForegroundInfo("Downloading: ${video.title}", 0))

        val outputDir = File(
            applicationContext.getExternalFilesDir(null),
            "playlists/${playlist.id}"
        ).also { it.mkdirs() }

        val processId = "download_${video.id}"

        return try {
            val localPath = downloadRepository.downloadVideo(
                videoUrl = "https://www.youtube.com/watch?v=${video.ytId}",
                outputDir = outputDir,
                config = playlist.config,
                processId = processId,
                onProgress = { percent, _ ->
                    kotlinx.coroutines.runBlocking {
                        playlistRepository.updateVideoDownloadState(
                            id = video.id,
                            status = "downloading",
                            progress = percent
                        )
                    }
                }
            )

            playlistRepository.updateVideoDownloadState(
                id = video.id,
                status = "downloaded",
                progress = 100,
                localPath = localPath,
                downloadedAt = System.currentTimeMillis()
            )

            val downloadedCount = playlistRepository.countDownloaded(playlistId)
            playlistRepository.updateSyncState(playlistId, System.currentTimeMillis(), downloadedCount)

            // Re-enqueue this slot if more videos remain
            if (playlistRepository.hasPendingVideos(playlistId)) {
                enqueueSlot(playlistId, slot)
            }

            Result.success()
        } catch (e: Exception) {
            playlistRepository.updateVideoDownloadState(
                id = video.id,
                status = "error",
                progress = 0,
                errorMessage = e.message
            )
            // Re-enqueue to continue with remaining videos even after a failure
            if (playlistRepository.hasPendingVideos(playlistId)) {
                enqueueSlot(playlistId, slot)
            }
            Result.success()
        }
    }

    private fun enqueueSlot(playlistId: String, slot: Int) {
        val request = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(workDataOf(
                KEY_PLAYLIST_ID to playlistId,
                KEY_SLOT to slot
            ))
            .setConstraints(PlaylistSyncCheckWorker.downloadConstraints())
            .addTag("download_$playlistId")
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "download_${playlistId}_slot_$slot",
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    private fun buildForegroundInfo(title: String, progress: Int): ForegroundInfo {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("PlaylistSync")
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Cancel",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
            )
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            applicationContext
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_PLAYLIST_ID = "playlist_id"
        const val KEY_SLOT        = "slot"
        const val CHANNEL_ID      = "playlist_sync_downloads"
        const val NOTIFICATION_ID = 1001
    }
}
