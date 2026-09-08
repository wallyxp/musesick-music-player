# Music (Material You & Zune-Inspired YouTube Music & Local Audio Player)

A modern Android music player built with **Jetpack Compose**, combining the expressive styling of **Material You (Dynamic System Color)** with a **Microsoft Zune-inspired artist tile mosaic**.

![Material You](https://img.shields.io/badge/Design-Material%20You%20(Dynamic%20Color)-purple?style=flat-square)
![UI Style](https://img.shields.io/badge/Home%20UI-Zune%20Artist%20Tiles-blue?style=flat-square)
![Android API](https://img.shields.io/badge/Android-API%2026%2B%20(Android%208.0%20to%2015)-black?style=flat-square)
![Zero Login](https://img.shields.io/badge/Login-Zero%20Login%20Required-black?style=flat-square)
![Formats](https://img.shields.io/badge/Formats-.MP3%20%7C%20.M4A%20%7C%20.FLAC-teal?style=flat-square)

---

## What's New & Key Features

- **Material You Design System**:
  - Automatically adapts to your Android system's wallpaper color scheme via **Material 3 Dynamic Color** (`dynamicDarkColorScheme` & `dynamicLightColorScheme`).
  - Seamlessly respects system Light & Dark mode settings.
- **Microsoft Zune-Inspired Artist Mosaic**:
  - Bold, cinematic mosaic tiles with responsive touch spring/scale animations.
  - Preloaded initial artists:
    - *Polyphia*
    - *Unprocessed*
    - *Animals as Leaders*
    - *Syncatto*
    - *Sleep Token*
    - *Bad Omens*
    - *Guns and Roses*
    - *AC/DC*
    - *Intervals*
    - *Plini*
    - *CHON*
    - *Ichika Nito*
    - *Linkin Park*
- **Customizable Artist Home**:
  - **"Change Artists"** button at the bottom of the home screen opens a management sheet.
  - Select/unselect which artists to display.
  - Add as many custom artists as you want.
- **Dedicated Artist Detail Page**:
  - Tapping an artist opens their full profile.
  - **Albums Section**: Displays 4 albums initially with a **"See More Albums"** button.
  - **Popular Songs Section**: Displays 15 songs initially with a **"See More Songs"** button.
- **Dedicated Album Detail Page**:
  - Tapping an album displays album artwork, release metadata, and the full tracklist.
  - Includes **"Play All"** and **"Shuffle"** action buttons.
- **Multi-Category Search**:
  - Search results are displayed in clean **lists** (not tiles) grouped into:
    1. **Artists**: Tapping opens the Artist Page.
    2. **Albums**: Tapping opens the Album Page.
    3. **Songs**: Tapping plays the track immediately.
- **Local Audio Support**:
  - Full playback support for **`.mp3`**, **`.m4a`**, and **`.flac`** audio files.
  - MediaStore scanning and system file picker fallback.
- **Background Audio & Media Notification**:
  - Persistent playback with Android MediaSession and lockscreen controls.

---

## Installation & Running

### 1. Install via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Open in Android Studio
Open the `/home/wally/Desktop/musicplayer-yt` folder in Android Studio and run on an emulator or connected device.

### 3. Rebuild from Terminal
```bash
export JAVA_HOME=/home/wally/.jdks/jbr-21.0.11
export ANDROID_HOME=/home/wally/Android/Sdk
./gradlew assembleDebug
```
The resulting APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
