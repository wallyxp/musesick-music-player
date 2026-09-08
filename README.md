# Music (Nothing Phone UI Inspired YouTube Music & Local Audio Player)

A minimalist, high-contrast Android music client built with Jetpack Compose, designed with the iconic **Nothing OS** / **Nothing Phone** design aesthetic.

![Nothing OS UI Aesthetic](https://img.shields.io/badge/UI-Nothing%20OS%20Aesthetic-red?style=flat-square)
![Android API](https://img.shields.io/badge/Android-API%2026%2B%20(Android%208.0%20to%2015)-black?style=flat-square)
![Zero Login](https://img.shields.io/badge/Login-Zero%20Login%20Required-black?style=flat-square)
![Formats](https://img.shields.io/badge/Formats-.MP3%20%7C%20.M4A%20%7C%20.FLAC-white?style=flat-square)

---

## Highlights

- **Zero-Login YouTube Music Streaming**:
  - Connects directly to YouTube Music's public InnerTube API.
  - Curated quick-vibe pills (`TOP HITS`, `LOFI BEATS`, `SYNTHWAVE`, `CHILL`, `INDIE`, `ROCK`, `AMBIENT`, `ELECTRONIC`) load instantly so music starts playing straight away with 1 tap.
  - Search any track, artist, or album.
- **Local Device Audio Playback**:
  - Automatically queries `MediaStore` for audio stored on your phone.
  - Dedicated format support for **`.mp3`**, **`.m4a`**, and **`.flac`** (with lossless tag indicators).
  - Built-in **"OPEN AUDIO FILES"** system document picker fallback to play audio files from any folder or SD card.
  - Filter by format: `ALL`, `FLAC`, `M4A`, `MP3` with real-time file counters.
- **Nothing OS Aesthetics**:
  - Pure OLED black `#000000` with subtle monochrome card surfaces and thin borders (`#262626`).
  - Signature Nothing red accents (`#D71921`) for indicators, record dots, and audio scrubber.
  - 5x7 Dot-matrix canvas typography for track counters, timecodes, format tags, and glyphs.
  - Animated Nothing audio equalizer visualizer & rotating Nothing disc glyph in the full player.
- **Background Playback & Lockscreen Controls**:
  - Android `ForegroundService` with `MediaSessionCompat`.
  - Notification player with Play / Pause, Previous, and Next actions that keep music playing when your phone is locked or screen is off.

---

## App Structure

```
musicplayer-yt/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/nothing/music/
│   │   │   ├── MainActivity.kt               # Edge-to-edge system bars, permission handling
│   │   │   ├── model/
│   │   │   │   ├── Track.kt                  # Unified YouTube and Local track model
│   │   │   │   └── PlaybackState.kt          # Reactive player state & progress
│   │   │   ├── repository/
│   │   │   │   ├── YouTubeRepository.kt      # Anonymous InnerTube search & categories
│   │   │   │   └── LocalAudioRepository.kt   # MediaStore & Storage picker for .mp3/.m4a/.flac
│   │   │   ├── player/
│   │   │   │   ├── PlayerManager.kt          # Dual playback engine & queue controller
│   │   │   │   └── MusicService.kt           # Foreground media service & notification
│   │   │   └── ui/
│   │   │       ├── MusicViewModel.kt         # Central UI ViewModel
│   │   │       ├── theme/                    # Nothing OS monochrome palette & typography
│   │   │       ├── components/
│   │   │       │   ├── DotMatrixText.kt      # Custom canvas 5x7 dot-matrix renderer
│   │   │       │   ├── VisualizerGlyph.kt    # Animated dot-matrix visualizer & disc glyph
│   │   │       │   ├── NothingButtons.kt     # Pill buttons, format badges, circular controls
│   │   │       │   ├── NothingSlider.kt      # Nothing red dot progress scrubber
│   │   │       │   └── MiniPlayerBar.kt      # Floating bottom bar with progress line
│   │   │       └── screens/
│   │   │           ├── MainScreen.kt         # Top bar (●) MUSIC, stream/local switch
│   │   │           ├── StreamTab.kt          # YouTube Music stream screen
│   │   │           ├── LocalTab.kt           # Local files (.mp3, .m4a, .flac) screen
│   │   │           └── FullPlayerSheet.kt    # Fullscreen Nothing OS player dialog
│   │   └── res/                              # Monochrome launcher & notification icons
└── app/build/outputs/apk/debug/app-debug.apk # Built APK ready to install
```

---

## Installation & Running

### 1. Install via ADB
Connect your phone with USB debugging enabled, then run:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Open in Android Studio
Open the `/home/wally/Desktop/musicplayer-yt` folder directly in Android Studio. Sync Gradle and press **Run** (Shift+F10) on an emulator or connected device.

### 3. Rebuild from CLI
```bash
export JAVA_HOME=/home/wally/.jdks/jbr-21.0.11
export ANDROID_HOME=/home/wally/Android/Sdk
./gradlew assembleDebug
```

