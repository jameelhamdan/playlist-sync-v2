package com.playlistsync

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PlaylistSyncApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        YoutubeDL.getInstance().init(this)
        FFmpeg.getInstance().init(this)
        updateYtDlpAsync()
    }

    private fun updateYtDlpAsync() {
        Thread {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel.STABLE)
            } catch (_: Exception) {}
        }.start()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
