package com.playlistsync.ui.config

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playlistsync.data.model.PlaylistConfig
import com.playlistsync.data.repository.PlaylistRepository
import com.playlistsync.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormatConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle[Screen.FormatConfig.ARG])

    private val _config = MutableStateFlow(PlaylistConfig())
    val config: StateFlow<PlaylistConfig> = _config

    init {
        viewModelScope.launch {
            playlistRepository.getById(playlistId)?.let {
                _config.value = it.config
            }
        }
    }

    fun updateConfig(updated: PlaylistConfig) {
        _config.value = updated
    }

    fun save() {
        viewModelScope.launch {
            playlistRepository.updateConfig(playlistId, _config.value)
        }
    }
}
