package com.playlistsync.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.playlistsync.data.model.PlaylistEntity
import com.playlistsync.data.repository.PlaylistRepository
import com.playlistsync.data.settings.AppSettingsRepository
import com.playlistsync.worker.PlaylistSyncCheckWorker
import com.playlistsync.worker.VideoDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistListViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val workManager: WorkManager,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = playlistRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun syncNow(playlist: PlaylistEntity) {
        // Run a one-time sync check for this playlist: fetches new videos then starts downloads
        val request = OneTimeWorkRequestBuilder<PlaylistSyncCheckWorker>()
            .setInputData(workDataOf(PlaylistSyncCheckWorker.KEY_PLAYLIST_ID to playlist.id))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .addTag("sync_${playlist.id}")
            .build()

        workManager.enqueueUniqueWork(
            "sync_now_${playlist.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun delete(playlist: PlaylistEntity) {
        viewModelScope.launch {
            workManager.cancelAllWorkByTag("download_${playlist.id}")
            workManager.cancelAllWorkByTag("sync_${playlist.id}")
            playlistRepository.delete(playlist)
        }
    }

    fun toggleAutoSync(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistRepository.update(playlist.copy(autoSyncEnabled = !playlist.autoSyncEnabled))
        }
    }
}
