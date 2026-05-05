package com.playlistsync.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.playlistsync.data.repository.DownloadRepository
import com.playlistsync.data.settings.AppSettings
import com.playlistsync.data.settings.AppSettingsRepository
import com.playlistsync.worker.PlaylistSyncCheckWorker

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class YtDlpUpdateStatus { IDLE, UPDATING, DONE, ERROR }

data class YtDlpState(
    val currentVersion: String = "",
    val updatedVersion: String = "",
    val status: YtDlpUpdateStatus = YtDlpUpdateStatus.IDLE,
    val error: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: AppSettingsRepository,
    private val downloadRepo: DownloadRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _ytDlp = MutableStateFlow(
        YtDlpState(currentVersion = downloadRepo.getVersion(context))
    )
    val ytDlpState: StateFlow<YtDlpState> = _ytDlp.asStateFlow()

    fun updateYtDlp() {
        _ytDlp.value = _ytDlp.value.copy(status = YtDlpUpdateStatus.UPDATING, error = "", updatedVersion = "")
        viewModelScope.launch {
            try {
                val updated = downloadRepo.updateYtDlp(context)
                val newVersion = downloadRepo.getVersion(context)
                _ytDlp.value = _ytDlp.value.copy(
                    status = YtDlpUpdateStatus.DONE,
                    currentVersion = newVersion,
                    updatedVersion = if (updated) newVersion else ""
                )
            } catch (e: Exception) {
                _ytDlp.value = _ytDlp.value.copy(
                    status = YtDlpUpdateStatus.ERROR,
                    error = e.message ?: "Update failed"
                )
            }
        }
    }

    fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.update(block)
            val s = settingsRepo.getSettings()
            workManager.enqueueUniquePeriodicWork(
                PlaylistSyncCheckWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PlaylistSyncCheckWorker.buildPeriodicRequest(s)
            )
        }
    }
}
