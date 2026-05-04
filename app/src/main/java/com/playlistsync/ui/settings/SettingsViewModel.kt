package com.playlistsync.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.playlistsync.data.settings.AppSettings
import com.playlistsync.data.settings.AppSettingsRepository
import com.playlistsync.worker.PlaylistSyncCheckWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: AppSettingsRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.update(block)
            rescheduleWorker()
        }
    }

    private suspend fun rescheduleWorker() {
        val s = settingsRepo.getSettings()
        workManager.enqueueUniquePeriodicWork(
            PlaylistSyncCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PlaylistSyncCheckWorker.buildPeriodicRequest(s)
        )
    }
}
