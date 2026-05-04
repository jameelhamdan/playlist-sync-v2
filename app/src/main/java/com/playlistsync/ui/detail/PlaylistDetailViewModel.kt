package com.playlistsync.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.playlistsync.data.model.PlaylistEntity
import com.playlistsync.data.model.VideoEntity
import com.playlistsync.data.repository.PlaylistRepository
import com.playlistsync.ui.navigation.Screen
import com.playlistsync.worker.PlaylistSyncCheckWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val playlistId: String = checkNotNull(savedStateHandle[Screen.Detail.ARG])

    val playlist: StateFlow<PlaylistEntity?> =
        playlistRepository.observeAll()
            .map { list -> list.find { it.id == playlistId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val videos: StateFlow<List<VideoEntity>> =
        playlistRepository.observeVideos(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun syncNow() {
        // Full sync: fetch updated metadata then start downloads
        val request = OneTimeWorkRequestBuilder<PlaylistSyncCheckWorker>()
            .setInputData(workDataOf(PlaylistSyncCheckWorker.KEY_PLAYLIST_ID to playlistId))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .addTag("sync_$playlistId")
            .build()

        workManager.enqueueUniqueWork(
            "sync_now_$playlistId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun retryVideo(videoId: String) {
        viewModelScope.launch {
            playlistRepository.updateVideoDownloadState(
                id = videoId,
                status = "pending",
                progress = 0,
                errorMessage = null
            )
            syncNow()
        }
    }
}
