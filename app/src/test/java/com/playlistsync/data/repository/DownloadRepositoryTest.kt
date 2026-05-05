package com.playlistsync.data.repository

import com.playlistsync.data.model.PlaylistConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRepositoryTest {

    // ── buildFormatString ────────────────────────────────────────────────────

    @Test
    fun `audio mode returns bestaudio fallback`() {
        val config = PlaylistConfig(syncMode = "audio")
        assertEquals("bestaudio/best", DownloadRepository.buildFormatString(config))
    }

    @Test
    fun `custom formatString is passed through unchanged`() {
        val config = PlaylistConfig(syncMode = "audio", formatString = "bestaudio[ext=opus]")
        assertEquals("bestaudio[ext=opus]", DownloadRepository.buildFormatString(config))
    }

    @Test
    fun `video 1080p produces correct height-capped format`() {
        val config = PlaylistConfig(syncMode = "video", qualityPreset = "1080p")
        val fmt = DownloadRepository.buildFormatString(config)
        assertTrue(fmt.contains("height<=1080"))
        assertTrue(fmt.startsWith("bestvideo"))
    }

    @Test
    fun `video 720p produces correct height-capped format`() {
        val config = PlaylistConfig(syncMode = "video", qualityPreset = "720p")
        val fmt = DownloadRepository.buildFormatString(config)
        assertTrue(fmt.contains("height<=720"))
    }

    @Test
    fun `video 480p produces correct height-capped format`() {
        val config = PlaylistConfig(syncMode = "video", qualityPreset = "480p")
        val fmt = DownloadRepository.buildFormatString(config)
        assertTrue(fmt.contains("height<=480"))
    }

    @Test
    fun `video 360p produces correct height-capped format`() {
        val config = PlaylistConfig(syncMode = "video", qualityPreset = "360p")
        val fmt = DownloadRepository.buildFormatString(config)
        assertTrue(fmt.contains("height<=360"))
    }

    @Test
    fun `video unknown preset falls back to best`() {
        val config = PlaylistConfig(syncMode = "video", qualityPreset = "best")
        assertEquals("bestvideo+bestaudio/best", DownloadRepository.buildFormatString(config))
    }

    // ── buildAudioMetadataArgs ───────────────────────────────────────────────

    @Test
    fun `all fields present builds full ffmpeg args`() {
        val args = DownloadRepository.buildAudioMetadataArgs("Artist", "Album", 3)
        assertNotNull(args)
        assertTrue(args!!.startsWith("ffmpeg:"))
        assertTrue(args.contains("album_artist=Artist"))
        assertTrue(args.contains("album=Album"))
        assertTrue(args.contains("track=3"))
    }

    @Test
    fun `channel name only skips album and track`() {
        val args = DownloadRepository.buildAudioMetadataArgs("Artist", "", 0)
        assertNotNull(args)
        assertTrue(args!!.contains("album_artist=Artist"))
        assertTrue(!args.contains("album="))
        assertTrue(!args.contains("track="))
    }

    @Test
    fun `all fields empty returns null`() {
        assertNull(DownloadRepository.buildAudioMetadataArgs("", "", 0))
    }

    @Test
    fun `single quotes in names are stripped to avoid shlex errors`() {
        val args = DownloadRepository.buildAudioMetadataArgs("O'Brien", "Let's Go", 1)
        assertNotNull(args)
        assertTrue(!args!!.contains("O'Brien"))
        assertTrue(!args.contains("Let's"))
        assertTrue(args.contains("OBrien"))
        assertTrue(args.contains("Lets Go"))
    }

    @Test
    fun `track number zero is omitted`() {
        val args = DownloadRepository.buildAudioMetadataArgs("", "Album", 0)
        assertNotNull(args)
        assertTrue(!args!!.contains("track="))
    }

    // ── extractPlaylistId ────────────────────────────────────────────────────

    @Test
    fun `list param in query string is extracted`() {
        val url = "https://www.youtube.com/playlist?list=PLxyz123ABC"
        assertEquals("PLxyz123ABC", DownloadRepository.extractPlaylistId(url))
    }

    @Test
    fun `list param after other params is extracted`() {
        val url = "https://www.youtube.com/watch?v=abc&list=PLabcDEF456"
        assertEquals("PLabcDEF456", DownloadRepository.extractPlaylistId(url))
    }

    @Test
    fun `url with no list param falls back to hash string`() {
        val url = "https://www.youtube.com/watch?v=abc123"
        val result = DownloadRepository.extractPlaylistId(url)
        assertEquals(url.hashCode().toString(), result)
    }

    // ── parsePlaylistJson ────────────────────────────────────────────────────

    @Test
    fun `valid playlist json is parsed correctly`() {
        val json = """
            {
              "id": "PLtest",
              "title": "My Playlist",
              "uploader": "Some Channel",
              "thumbnail": "https://img.example.com/thumb.jpg",
              "entries": [
                { "id": "vid1", "title": "Video One", "duration": 120, "thumbnail": "https://t1.jpg" },
                { "id": "vid2", "title": "Video Two", "duration": 240, "thumbnail": "https://t2.jpg" }
              ]
            }
        """.trimIndent()

        val meta = DownloadRepository.parsePlaylistJson(json, "https://fallback.url")

        assertEquals("PLtest", meta.id)
        assertEquals("My Playlist", meta.title)
        assertEquals("Some Channel", meta.channelName)
        assertEquals("https://img.example.com/thumb.jpg", meta.thumbnailUrl)
        assertEquals(2, meta.entries.size)
        assertEquals("vid1", meta.entries[0].ytId)
        assertEquals("Video One", meta.entries[0].title)
        assertEquals(120L, meta.entries[0].duration)
        assertEquals(0, meta.entries[0].playlistIndex)
        assertEquals("vid2", meta.entries[1].ytId)
        assertEquals(1, meta.entries[1].playlistIndex)
    }

    @Test
    fun `entries with missing id are skipped`() {
        val json = """
            {
              "id": "PL1",
              "title": "Test",
              "entries": [
                { "id": "", "title": "No ID" },
                { "id": "valid1", "title": "Has ID" }
              ]
            }
        """.trimIndent()

        val meta = DownloadRepository.parsePlaylistJson(json, "")
        assertEquals(1, meta.entries.size)
        assertEquals("valid1", meta.entries[0].ytId)
    }

    @Test
    fun `missing optional fields use defaults`() {
        val json = """{ "entries": [] }"""
        val meta = DownloadRepository.parsePlaylistJson(json, "https://youtube.com/playlist?list=PLfallback")

        assertEquals("PLfallback", meta.id)
        assertEquals("Unknown Playlist", meta.title)
        assertEquals("", meta.channelName)
        assertEquals("", meta.thumbnailUrl)
        assertTrue(meta.entries.isEmpty())
    }

    @Test
    fun `channel field used when uploader is absent`() {
        val json = """{ "id": "PL1", "channel": "Chan Name", "entries": [] }"""
        val meta = DownloadRepository.parsePlaylistJson(json, "")
        assertEquals("Chan Name", meta.channelName)
    }
}
