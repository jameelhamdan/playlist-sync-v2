# Running PlaylistSync

## Device Setup (Physical Android Device)

### 1. Enable Developer Options

1. Open **Settings → About phone**
2. Tap **Build number** 7 times until "You are now a developer!" appears
3. Go back to **Settings → Developer options**
4. Enable **USB debugging**

### 2. Connect via USB

```bash
# Verify device is detected
adb devices
# Should show:  <serial>  device
```

If it shows `unauthorized`, accept the RSA key fingerprint prompt on the device.

### 3. Install and run

```bash
./gradlew :app:installDebug
adb shell am start -n com.playlistsync/.MainActivity
```

### 4. View logs

```bash
adb logcat -s "PlaylistSync" "YoutubeDL" "WorkManager"
```

---

## Emulator Setup

### Using Android Studio AVD Manager (GUI)

1. Open Android Studio → **Device Manager** → **Create Virtual Device**
2. Select a phone profile (e.g. Pixel 8)
3. Select API 35 system image (download if needed)
4. Click Finish, then the ▶ play button

### Using command line

```bash
# List available system images
sdkmanager --list | grep "system-images;android-35"

# Install one
sdkmanager "system-images;android-35;google_apis;arm64-v8a"

# Create AVD
avdmanager create avd \
  --name Pixel8_API35 \
  --package "system-images;android-35;google_apis;arm64-v8a" \
  --device "pixel_8"

# Start emulator
emulator -avd Pixel8_API35 &

# Wait for boot, then install
adb wait-for-device
./gradlew :app:installDebug
```

---

## ADB Sideload (no USB, over WiFi)

### Android 11+ wireless debugging

1. **Settings → Developer options → Wireless debugging** → Enable
2. Tap **Pair device with pairing code**
3. Note the IP:port and pairing code shown on screen

```bash
adb pair <IP>:<port>
# Enter the 6-digit pairing code when prompted

adb connect <IP>:<debug-port>
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Granting Permissions

The app requests `POST_NOTIFICATIONS` at launch (Android 13+). If dismissed:

```bash
adb shell pm grant com.playlistsync android.permission.POST_NOTIFICATIONS
```

---

## Troubleshooting

### Build fails: "SDK location not found"

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

### `./gradlew` is not executable

```bash
chmod +x gradlew
```

### First build is slow

The first build downloads:
- Gradle 8.11.1 distribution (~120 MB)
- All Maven dependencies including yt-dlp native binaries (~250 MB total)

Subsequent builds use the Gradle cache and are much faster.

### yt-dlp errors at runtime

yt-dlp is bundled in the APK but can be updated without a new app release. To update via ADB shell or from the settings screen (if you add an update button):

```kotlin
YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
```

### Checking downloaded files

```bash
adb shell ls /sdcard/Android/data/com.playlistsync/files/playlists/
```
