# PlaylistSync

Native Android app that syncs and downloads YouTube playlists via yt-dlp.

## Stack

- **Language:** Kotlin + Jetpack Compose
- **DI:** Hilt
- **DB:** Room
- **Background work:** WorkManager
- **Download engine:** youtubedl-android (yt-dlp wrapper)

## Build

```bash
./gradlew :app:assembleDebug
```

## Tests

JVM unit tests only — no emulator or device needed.

```bash
# All tests
./scripts/test.sh

# Single class
./scripts/test.sh DownloadRepositoryTest
```

Each run appends a timestamped log to `results/<YYYYMMDD_HHMMSS>.log` (gitignored).

### Instrumented tests (real Kotlin code on device)

Runs `DownloadRepository` via the actual `youtubedl-android` library on a connected device or emulator. Tests metadata fetch, audio download, and video download end-to-end.

```bash
./scripts/test-instrumented.sh
```

Requires a connected device (`adb devices`). HTML report at `app/build/reports/androidTests/connected/debug/index.html`.

### Download script

Downloads a full playlist using the exact yt-dlp flags from `DownloadRepository`. Requires `yt-dlp` (`brew install yt-dlp`).

```bash
./scripts/download.sh                        # audio, default playlist
./scripts/download.sh audio <url>            # audio, custom playlist
./scripts/download.sh video <url>            # video, custom playlist
```

Config overrideable via env vars: `AUDIO_FORMAT`, `VIDEO_QUALITY`, `VIDEO_CONTAINER`, `EMBED_THUMBNAIL`, `CONCURRENT`.

Default playlist: `https://www.youtube.com/playlist?list=PL59FEE129ADFF2B12`

Artifacts saved to `results/<YYYYMMDD_HHMMSS>-download.log` and files in `results/<YYYYMMDD_HHMMSS>-files/` (all gitignored).

Tests live in `app/src/test/`. Three test files cover the core download pipeline:

| File | What it tests |
|------|---------------|
| `DownloadRepositoryTest` | Format string generation, metadata args, URL parsing, JSON parsing |
| `DownloadArgsTest` | Audio vs video yt-dlp option selection (extract audio, embed thumbnail, subs, aria2c, extra args) |
| `AddPlaylistViewModelTest` | PlaylistEntity + VideoEntity construction from metadata and settings |

Pure logic is exposed via `internal` companion object functions — no mocking framework needed.

## Key files

- `DownloadRepository.kt` — yt-dlp request construction + pure format/metadata helpers
- `VideoDownloadWorker.kt` — WorkManager worker that drives downloads
- `PlaylistSyncCheckWorker.kt` — periodic sync check worker
- `AppSettings.kt` / `AppSettingsRepository.kt` — DataStore-backed settings
