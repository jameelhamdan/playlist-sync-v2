package com.playlistsync.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.playlistsync.data.model.PlaylistConfig
import com.playlistsync.data.model.PlaylistEntity
import com.playlistsync.data.model.VideoEntity
import com.playlistsync.data.repository.DownloadRepository
import com.playlistsync.data.repository.PlaylistMetadata
import com.playlistsync.data.repository.PlaylistRepository
import com.playlistsync.data.settings.AppSettingsRepository
import com.playlistsync.worker.PlaylistSyncCheckWorker
import com.playlistsync.worker.VideoDownloadWorker
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
    private val workManager: WorkManager,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddPlaylistUiState>(AddPlaylistUiState.Idle)
    val uiState: StateFlow<AddPlaylistUiState> = _uiState

    val urlInput = MutableStateFlow("")

    private var fetchedMetadata: PlaylistMetadata? = null

    init {
        viewModelScope.launch {
            val defaultMode = appSettingsRepository.getSettings().defaultSyncMode
            val current = _uiState.value
            if (current is AddPlaylistUiState.Preview) {
                _uiState.value = current.copy(syncMode = defaultMode)
            }
        }
    }

    fun fetchPlaylistInfo() {
        val url = urlInput.value.trim()
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AddPlaylistUiState.Loading
            try {
                val settings = appSettingsRepository.getSettings()
                val meta = downloadRepository.fetchPlaylistMetadata(url, settings.defaultProxyUrl)
                fetchedMetadata = meta
                _uiState.value = AddPlaylistUiState.Preview(
                    name = meta.title,
                    videoCount = meta.entries.size,
                    channelName = meta.channelName,
                    syncMode = settings.defaultSyncMode
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
            val settings = appSettingsRepository.getSettings()

            val entity = PlaylistEntity(
                id = meta.id,
                url = urlInput.value.trim(),
                name = meta.title,
                thumbnailUrl = meta.thumbnailUrl,
                channelName = meta.channelName,
                videoCount = meta.entries.size,
                config = PlaylistConfig(
                    syncMode = preview.syncMode,
                    audioFormat = settings.defaultAudioFormat,
                    qualityPreset = settings.defaultVideoQuality,
                    embedThumbnail = settings.defaultEmbedThumbnail,
                    maxConcurrentDownloads = settings.defaultConcurrentDownloads,
                    proxyUrl = settings.defaultProxyUrl
                )
            )
            playlistRepository.save(entity)

            // Insert all fetched video metadata immediately so they appear right away
            val videoEntities = meta.entries.map { entry ->
                VideoEntity(
                    id = "${entity.id}/${entry.ytId}",
                    playlistId = entity.id,
                    ytId = entry.ytId,
                    title = entry.title,
                    duration = entry.duration,
                    thumbnailUrl = entry.thumbnailUrl,
                    playlistIndex = entry.playlistIndex,
                    status = "pending"
                )
            }
            playlistRepository.insertVideos(videoEntities)

            // Start download slots immediately
            repeat(settings.defaultConcurrentDownloads) { slot ->
                val dlRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
                    .setInputData(workDataOf(
                        VideoDownloadWorker.KEY_PLAYLIST_ID to entity.id,
                        VideoDownloadWorker.KEY_SLOT to slot
                    ))
                    .setConstraints(PlaylistSyncCheckWorker.downloadConstraints(settings))
                    .addTag("download_${entity.id}")
                    .build()
                workManager.enqueueUniqueWork(
                    "download_${entity.id}_slot_$slot",
                    ExistingWorkPolicy.KEEP,
                    dlRequest
                )
            }

            // Register the periodic background sync worker
            workManager.enqueueUniquePeriodicWork(
                PlaylistSyncCheckWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PlaylistSyncCheckWorker.buildPeriodicRequest(settings)
            )

            _uiState.value = AddPlaylistUiState.Saved
        }
    }

    fun resetState() {
        _uiState.value = AddPlaylistUiState.Idle
        fetchedMetadata = null
    }
}
