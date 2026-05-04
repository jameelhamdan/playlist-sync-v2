package com.playlistsync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.playlistsync.data.model.VideoEntity
import com.playlistsync.data.repository.DownloadRepository
import com.playlistsync.data.repository.PlaylistRepository
import com.playlistsync.data.settings.AppSettings
import com.playlistsync.data.settings.AppSettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class PlaylistSyncCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val playlistRepository: PlaylistRepository,
    private val downloadRepository: DownloadRepository,
    private val appSettingsRepository: AppSettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val specificId = inputData.getString(KEY_PLAYLIST_ID)
        val playlists = if (specificId != null) {
            listOfNotNull(playlistRepository.getById(specificId))
        } else {
            playlistRepository.getAllAutoSync()
        }

        playlists.forEach { playlist ->
            try {
                val metadata = downloadRepository.fetchPlaylistMetadata(
                    playlist.url, playlist.config.proxyUrl
                )
                val knownIds = playlistRepository.getKnownYtIds(playlist.id).toSet()

                val newVideos = metadata.entries
                    .filter { it.ytId !in knownIds }
                    .map { entry ->
                        VideoEntity(
                            id = "${playlist.id}/${entry.ytId}",
                            playlistId = playlist.id,
                            ytId = entry.ytId,
                            title = entry.title,
                            duration = entry.duration,
                            thumbnailUrl = entry.thumbnailUrl,
                            playlistIndex = entry.playlistIndex,
                            status = "pending"
                        )
                    }

                playlistRepository.insertVideos(newVideos)
                playlistRepository.update(
                    playlist.copy(
                        videoCount = metadata.entries.size,
                        name = if (playlist.name.isEmpty()) metadata.title else playlist.name
                    )
                )

                if (newVideos.isNotEmpty() || playlistRepository.hasPendingVideos(playlist.id)) {
                    enqueueDownloadSlots(playlist.id, playlist.config.maxConcurrentDownloads)
                }
            } catch (_: Exception) {
                // Continue with remaining playlists if one fails
            }
        }
        return Result.success()
    }

    private suspend fun enqueueDownloadSlots(playlistId: String, concurrency: Int) {
        val settings = appSettingsRepository.getSettings()
        val wm = WorkManager.getInstance(applicationContext)
        repeat(concurrency) { slot ->
            val request = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
                .setInputData(workDataOf(
                    VideoDownloadWorker.KEY_PLAYLIST_ID to playlistId,
                    VideoDownloadWorker.KEY_SLOT to slot
                ))
                .setConstraints(downloadConstraints(settings))
                .addTag("download_$playlistId")
                .build()
            wm.enqueueUniqueWork(
                "download_${playlistId}_slot_$slot",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    companion object {
        const val WORK_NAME       = "playlist_sync_check"
        const val KEY_PLAYLIST_ID = "playlist_id"

        fun buildPeriodicRequest(settings: AppSettings = AppSettings()): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<PlaylistSyncCheckWorker>(
                settings.syncIntervalHours.toLong(), TimeUnit.HOURS
            )
                .setConstraints(buildConstraints(settings))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()

        fun buildConstraints(settings: AppSettings = AppSettings()): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresBatteryNotLow(settings.requireBatteryNotLow)
                .build()

        fun downloadConstraints(settings: AppSettings = AppSettings()): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()
    }
}
