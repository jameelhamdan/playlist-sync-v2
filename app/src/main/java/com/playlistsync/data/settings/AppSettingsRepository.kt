package com.playlistsync.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_SYNC_INTERVAL         = intPreferencesKey("sync_interval_hours")
        val KEY_WIFI_ONLY             = booleanPreferencesKey("wifi_only")
        val KEY_REQUIRE_BATTERY       = booleanPreferencesKey("require_battery_not_low")
        val KEY_DEFAULT_SYNC_MODE     = stringPreferencesKey("default_sync_mode")
        val KEY_DEFAULT_AUDIO_FORMAT  = stringPreferencesKey("default_audio_format")
        val KEY_DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val KEY_DEFAULT_EMBED_THUMB   = booleanPreferencesKey("default_embed_thumbnail")
        val KEY_DEFAULT_CONCURRENT    = intPreferencesKey("default_concurrent_downloads")
        val KEY_DEFAULT_PROXY         = stringPreferencesKey("default_proxy_url")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            syncIntervalHours         = prefs[KEY_SYNC_INTERVAL]         ?: 6,
            wifiOnly                  = prefs[KEY_WIFI_ONLY]              ?: false,
            requireBatteryNotLow      = prefs[KEY_REQUIRE_BATTERY]        ?: true,
            defaultSyncMode           = prefs[KEY_DEFAULT_SYNC_MODE]      ?: "audio",
            defaultAudioFormat        = prefs[KEY_DEFAULT_AUDIO_FORMAT]   ?: "m4a",
            defaultVideoQuality       = prefs[KEY_DEFAULT_VIDEO_QUALITY]  ?: "best",
            defaultEmbedThumbnail     = prefs[KEY_DEFAULT_EMBED_THUMB]    ?: true,
            defaultConcurrentDownloads = prefs[KEY_DEFAULT_CONCURRENT]    ?: 2,
            defaultProxyUrl           = prefs[KEY_DEFAULT_PROXY]          ?: ""
        )
    }

    suspend fun getSettings(): AppSettings = settings.first()

    suspend fun update(block: (AppSettings) -> AppSettings) {
        val updated = block(getSettings())
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_INTERVAL]         = updated.syncIntervalHours
            prefs[KEY_WIFI_ONLY]             = updated.wifiOnly
            prefs[KEY_REQUIRE_BATTERY]       = updated.requireBatteryNotLow
            prefs[KEY_DEFAULT_SYNC_MODE]     = updated.defaultSyncMode
            prefs[KEY_DEFAULT_AUDIO_FORMAT]  = updated.defaultAudioFormat
            prefs[KEY_DEFAULT_VIDEO_QUALITY] = updated.defaultVideoQuality
            prefs[KEY_DEFAULT_EMBED_THUMB]   = updated.defaultEmbedThumbnail
            prefs[KEY_DEFAULT_CONCURRENT]    = updated.defaultConcurrentDownloads
            prefs[KEY_DEFAULT_PROXY]         = updated.defaultProxyUrl
        }
    }
}
