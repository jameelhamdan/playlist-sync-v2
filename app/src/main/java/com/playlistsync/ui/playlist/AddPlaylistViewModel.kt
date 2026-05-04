package com.playlistsync.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.playlistsync.data.model.PlaylistEntity
import com.playlistsync.data.repository.DownloadRepository
import com.playlistsync.data.repository.PlaylistMetadata
import com.playlistsync.data.repository.PlaylistRepository
import com.playlistsync.worker.PlaylistSyncCheckWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AddPlaylistUiState {
    object Idle    : AddPlaylistUiState
    object Loading : AddPlaylistUiState
    data class Preview(
        val name: String,
        val videoCount: Int,
        val channelName: String,
        val syncMode: String = "audio"
    ) : AddPlaylistUiState
    data class Error(val message: String) : AddPlaylistUiState
    object Saved   : AddPlaylistUiState
}

@HiltViewModel
class AddPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val downloadRepository: DownloadRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddPlaylistUiState>(AddPlaylistUiState.Idle)
    val uiState: StateFlow<AddPlaylistUiState> = _uiState

    val urlInput = MutableStateFlow("")

    private var fetchedMetadata: PlaylistMetadata? = null

    fun fetchPlaylistInfo() {
        val url = urlInput.value.trim()
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AddPlaylistUiState.Loading
            try {
                val meta = downloadRepository.fetchPlaylistMetadata(url)
                fetchedMetadata = meta
                _uiState.value = AddPlaylistUiState.Preview(
                    name = meta.title,
                    videoCount = meta.entries.size,
                    channelName = meta.channelName
                )
            } catch (e: Exception) {
                _uiState.value = AddPlaylistUiState.Error(
                    e.message?.take(200) ?: "Failed to fetch playlist info"
                )
            }
        }
    }

    fun setSyncMode(mode: String) {
        val current = _uiState.value as? AddPlaylistUiState.Preview ?: return
        _uiState.value = current.copy(syncMode = mode)
    }

    fun savePlaylist() {
        val meta = fetchedMetadata ?: return
        val preview = _uiState.value as? AddPlaylistUiState.Preview ?: return
        viewModelScope.launch {
            val entity = PlaylistEntity(
                id = meta.id,
                url = urlInput.value.trim(),
                name = meta.title,
                thumbnailUrl = meta.thumbnailUrl,
                channelName = meta.channelName,
                videoCount = meta.entries.size,
                config = com.playlistsync.data.model.PlaylistConfig(syncMode = preview.syncMode)
            )
            playlistRepository.save(entity)

            workManager.enqueueUniquePeriodicWork(
                PlaylistSyncCheckWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PlaylistSyncCheckWorker.buildPeriodicRequest()
            )

            _uiState.value = AddPlaylistUiState.Saved
        }
    }

    fun resetState() {
        _uiState.value = AddPlaylistUiState.Idle
        fetchedMetadata = null
    }
}
