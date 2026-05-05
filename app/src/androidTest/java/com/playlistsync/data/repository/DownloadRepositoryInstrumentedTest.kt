package com.playlistsync.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.playlistsync.data.model.PlaylistConfig
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // runs in order: 1_update → 2_metadata → 3_format → 4_download
class DownloadRepositoryInstrumentedTest {

    companion object {
        private const val PLAYLIST_URL =
            "https://www.youtube.com/playlist?list=PL59FEE129ADFF2B12"

        private lateinit var repo: DownloadRepository
        private lateinit var outputDir: File

        // populated by test 2, used by tests 3 and 4
        private var firstVideoId: String = ""

        @BeforeClass
        @JvmStatic
        fun setup() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            YoutubeDL.getInstance().init(context)
            repo = DownloadRepository()
            outputDir = File(context.cacheDir, "instrumented-downloads").also { it.mkdirs() }
        }
    }

    // ── 1. update yt-dlp first so format selectors are current ───────────────

    @Test
    fun test1_updateYtDlp() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val status = repo.updateYtDlp(context)
        // ALREADY_UP_TO_DATE or DONE are both fine; anything else is unexpected
        assertTrue(
            "yt-dlp update returned unexpected status: $status",
            status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE ||
            status == YoutubeDL.UpdateStatus.DONE
        )
    }

    // ── 2. metadata ───────────────────────────────────────────────────────────

    @Test
    fun test2_fetchPlaylistMetadata() = runTest {
        val meta = repo.fetchPlaylistMetadata(PLAYLIST_URL)

        assertTrue("playlist title should not be blank", meta.title.isNotBlank())
        assertTrue("playlist should have entries", meta.entries.isNotEmpty())
        meta.entries.forEach { assertTrue("every video needs an id", it.ytId.isNotBlank()) }

        firstVideoId = meta.entries.first().ytId
    }

    // ── 3. format check — catches "Requested format not available" early ──────

    @Test
    fun test3_audioFormatAvailable() = runTest {
        check(firstVideoId.isNotBlank()) { "run test2 first" }
        val url = "https://www.youtube.com/watch?v=$firstVideoId"
        val config = PlaylistConfig(syncMode = "audio", audioFormat = "m4a")

        // throws YoutubeDLException if the format can't be resolved — no download happens
        repo.checkFormat(url, config)
    }

    @Test
    fun test3_videoFormatAvailable() = runTest {
        check(firstVideoId.isNotBlank()) { "run test2 first" }
        val url = "https://www.youtube.com/watch?v=$firstVideoId"
        val config = PlaylistConfig(syncMode = "video", qualityPreset = "720p", videoContainer = "mp4")

        repo.checkFormat(url, config)
    }

    // ── 4. actual download ────────────────────────────────────────────────────

    @Test
    fun test4_downloadAudio() = runTest {
        check(firstVideoId.isNotBlank()) { "run test2 first" }
        val dir = File(outputDir, "audio").also { it.mkdirs() }
        val config = PlaylistConfig(syncMode = "audio", audioFormat = "m4a", embedThumbnail = true)

        val path = repo.downloadVideo(
            videoUrl = "https://www.youtube.com/watch?v=$firstVideoId",
            outputDir = dir,
            config = config,
            processId = "test_audio_$firstVideoId",
            onProgress = { _, _ -> }
        )

        assertTrue("path should not be blank", path.isNotBlank())
        val file = File(path)
        assertTrue("file should exist", file.exists())
        assertTrue("file should not be empty", file.length() > 0)
        assertTrue("expected .m4a, got: $path", path.endsWith(".m4a"))
    }

    @Test
    fun test4_downloadVideo() = runTest {
        check(firstVideoId.isNotBlank()) { "run test2 first" }
        val dir = File(outputDir, "video").also { it.mkdirs() }
        val config = PlaylistConfig(syncMode = "video", qualityPreset = "720p", videoContainer = "mp4")

        val path = repo.downloadVideo(
            videoUrl = "https://www.youtube.com/watch?v=$firstVideoId",
            outputDir = dir,
            config = config,
            processId = "test_video_$firstVideoId",
            onProgress = { _, _ -> }
        )

        assertTrue("path should not be blank", path.isNotBlank())
        val file = File(path)
        assertTrue("file should exist", file.exists())
        assertTrue("file should not be empty", file.length() > 0)
    }
}
