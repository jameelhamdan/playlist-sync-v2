# PlaylistSync

Native Android app that saves, syncs, and downloads YouTube playlists using [yt-dlp](https://github.com/yt-dlp/yt-dlp) — no external servers required.

## Features

- **Playlist management** — Add YouTube playlist URLs; metadata (title, video count, channel) is fetched automatically via yt-dlp
- **Audio or Video mode** — Per-playlist toggle: download as audio-only (m4a/mp3/opus/flac/wav) or video+audio (mp4/webm/mkv)
- **Format configuration** — Per-playlist quality presets, container formats, embed thumbnail/subtitle options, aria2c multi-connection mode, and full custom yt-dlp format strings
- **Parallel downloads** — Configurable 1–5 concurrent download slots per playlist (default: 2); each slot runs as an independent WorkManager chain so large playlists (1000+ videos) drain efficiently
- **Proxy support** — Per-playlist HTTP/HTTPS/SOCKS5 proxy URL passed directly to yt-dlp; useful for geo-restricted content or avoiding rate limits
- **Login-required bypass** — Uses `youtube:player_client=android,web` extractor args so public playlists download without YouTube login prompts
- **Background sync** — WorkManager periodic job (every 6 hours) detects new playlist videos and downloads them automatically
- **Download notifications** — Foreground service notification with per-video progress and cancel button
- **Offline-first** — All data in local Room database; files in app-private storage (no storage permissions needed)

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 17+ |
| Android SDK | Build-tools 34+, Platform android-34+ |
| `ANDROID_HOME` | Set to your SDK root |

### Install Android SDK (if needed)

```bash
# macOS via Homebrew
brew install --cask android-commandlinetools

sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

## Build

```bash
# Clone and enter project
git clone https://github.com/jameelhamdan/playlist-sync-v2.git
cd playlist-sync-v2

# Set Android SDK path (if not already set)
export ANDROID_HOME=~/Library/Android/sdk   # macOS default
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Build debug APK  (~first build downloads Gradle + all dependencies, takes a few minutes)
./gradlew :app:assembleDebug

# APK output
ls app/build/outputs/apk/debug/app-debug.apk
```

## Install & Run

```bash
# Install on connected device or running emulator
./gradlew :app:installDebug

# Or install manually via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.playlistsync/.MainActivity
```

> See [docs/RUNNING.md](docs/RUNNING.md) for emulator setup, device enable instructions, and ADB sideloading.

## Usage

1. **Add a playlist** — Tap **+**, paste a YouTube playlist URL, tap **Fetch Playlist Info**, then **Add Playlist**
2. **Configure format** — From the playlist detail screen, tap the ⚙️ icon to set audio/video mode, quality, container, parallel download slots (1–5), proxy URL, and advanced yt-dlp args
3. **Sync** — Tap the 🔄 icon on any playlist to download pending videos immediately; or leave auto-sync enabled for background downloads every 6 hours
4. **Monitor progress** — Download notifications show per-video progress; the detail screen shows per-video status (Pending / Downloading / Downloaded / Error)

## Storage

Downloaded files are stored at:

```
Android/data/com.playlistsync/files/playlists/{playlistId}/{title} [{ytId}].{ext}
```

No storage permissions are required (app-private external storage, Android 8.0+).

## Build Variants

```bash
./gradlew :app:assembleDebug        # Debug APK (unoptimized, debuggable)
./gradlew :app:assembleRelease      # Release APK (ProGuard enabled, unsigned)
./gradlew :app:bundleRelease        # AAB for Play Store submission
./gradlew clean                     # Clean build outputs
```

## Architecture

```
UI (Jetpack Compose)
  └── ViewModels (Hilt + StateFlow)
        └── Repositories (Room DAOs + DownloadRepository)
              └── WorkManager Workers (background sync + download)
                    └── youtubedl-android (yt-dlp + FFmpeg + aria2c)
```

- **DownloadRepository** wraps `YoutubeDL.getInstance()` calls; both `fetchPlaylistMetadata()` and `downloadVideo()` pass `--extractor-args "youtube:player_client=android,web"` to avoid login-required errors on public content; proxy and socket-timeout flags are forwarded from `PlaylistConfig`
- **PlaylistSyncCheckWorker** (PeriodicWorkRequest, 6h) — compares known video IDs against the remote playlist, inserts new `VideoEntity` rows, then starts `config.maxConcurrentDownloads` parallel download slots
- **VideoDownloadWorker** (OneTimeWorkRequest, slot-based) — each slot atomically claims the next pending video via a Room `@Transaction` (preventing races between concurrent workers), downloads it as a foreground service, then re-enqueues itself for the next pending video; a 1000-video playlist with 3 slots runs 3 videos simultaneously throughout
