# Musesick Music Player — Complete Technical & Architectural Documentation

**Package Name:** `com.wally.musesick`
**Current Version:** `1.1.4` (`versionCode = 9`)
**Target / Compile SDK:** Android 15 (`API 35`), Minimum SDK: Android 8.0 (`API 26`)

---

## 1. Architectural Overview

Musesick is a hybrid **YouTube Music Streaming + Local Hi-Res Audio Player** built natively for Android using **Kotlin** and **Jetpack Compose (Material 3)**. It follows a clean **MVVM (Model–View–ViewModel)** + **Repository Pattern** architecture backed by a foreground **MediaSession Service** for background playback, lock-screen controls, and hardware media button support.

```
┌───────────────────────────────────────────────────────────────────────────┐
│                          UI Layer (Jetpack Compose)                       │
│  MainActivity -> MainScreen -> Tabs (ForYou,Playlists,Local) & SubScreens │
│  FullPlayerSheet (5 Player Styles), LyricsView, Dialogs, BottomSheets     │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │ StateFlow / Unidirectional Events
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      State & Presentation Layer                           │
│                        MusicViewModel (AndroidViewModel)                  │
└───────┬─────────────┬───────────────┬──────────────┬──────────────┬───────┘
        │             │               │              │              │
        ▼             ▼               ▼              ▼              ▼
┌──────────────┐ ┌──────────┐ ┌──────────────┐ ┌───────────┐ ┌──────────────┐
│PlayerManager │ │YouTube   │ │Playlist /    │ │LyricsRepo │ │UpdateManager │
│& MusicService│ │Repository│ │Local / Prefs │ │(LRCLib)   │ │(GitHub API)  │
│(MediaSession)│ │(InnerTube│ │Repositories  │ │           │ │              │
└──────────────┘ └──────────┘ └──────────────┘ └───────────┘ └──────────────┘
```

---

## 2. Complete Technology Stack & Libraries

| Category | Technology / Library | Version / Details | Purpose in Project |
| :--- | :--- | :--- | :--- |
| **Language & Runtime** | **Kotlin** + **Java 21 JVM Target** | Kotlin `2.x` / JDK `21` | Primary programming language, coroutines, data classes, sealed states. |
| **UI Toolkit** | **Jetpack Compose (BOM)** | `2024.09.02` | 100% declarative UI across all screens, sheets, animations, and custom canvases. |
| **Design System** | **Material 3 (`androidx.compose.material3`)** | Material 3 + Extended Icons | Dynamic theming, cards, sliders, sheets, dialogs, and adaptive palettes. |
| **State Management** | **Kotlin Coroutines & `StateFlow`** | `kotlinx-coroutines-android:1.8.1` | Asynchronous I/O (`Dispatchers.IO`), parallel requests (`async`/`coroutineScope`), reactive UI state streams. |
| **Lifecycle & MVVM** | **AndroidX Lifecycle & ViewModel Compose** | `2.8.6` | `MusicViewModel` survives configuration changes and coordinates all repositories. |
| **Media & Notifications** | **AndroidX Media (`MediaSessionCompat`)** + `MediaPlayer` | `androidx.media:media:1.7.0` | System media notification (`MediaStyle`), lock-screen transport controls, Bluetooth/headset button handling, audio focus. |
| **Image Loading** | **Coil Compose (`io.coil-kt:coil-compose`)** | `2.7.0` | Asynchronous loading, caching (`MemoryCache` + `DiskCache`), and crossfading of YouTube thumbnails and local album art. |
| **Networking** | **`java.net.HttpURLConnection` + `org.json`** | Native Android SDK | Zero-overhead REST & InnerTube JSON POST/GET calls, custom header signing (`SAPISIDHASH`), and streaming APK downloads. |
| **Authentication** | **Android `WebView` + `CookieManager` + `SAPISIDHASH`** | Native WebKit + SHA-1 | Captures YouTube Music session cookies (`SAPISID`, `__Secure-3PAPISID`, `SID`) and generates SHA-1 `SAPISIDHASH` authorization headers. |
| **Local Storage** | **Android `SharedPreferences` + Internal File Storage** | JSON serialization via `org.json` | Persists playlists, favorite artists, play counts, search history, recently played tracks, theme settings, and custom images. |
| **Local Audio Scanner** | **Android `MediaStore.Audio` + `MediaMetadataRetriever`** | ContentResolver / SAF | Queries device audio files (`FLAC`, `WAV`, `ALAC`, `MP3`, `M4A`, `OGG`) and extracts embedded ID3/Vorbis cover art. |
| **Unit Testing** | **JUnit 4** | `junit:4.13.2` | Unit test suite for LRC parsing, lyrics cleaning, playlist serialization, and YouTube URL parsing. |

---

## 3. Complete List of API Endpoints & External URLs

### 3.1. YouTube & YouTube Music InnerTube APIs (in `YouTubeRepository.kt`)
All InnerTube requests use `POST` with `Content-Type: application/json`, `X-Goog-Api-Format-Version: 2`, `X-Goog-AuthUser: 0`, and when logged in, both the `Cookie` header and the computed `Authorization: SAPISIDHASH <timestamp>_<sha1(timestamp + " " + sapisid + " " + origin)>` header.

| Endpoint URL | HTTP Method | Client Context(s) Used | Purpose |
| :--- | :--- | :--- | :--- |
| `https://music.youtube.com/youtubei/v1/search` | `POST` | `WEB_REMIX` (`1.20240101.01.00`) | Searches songs, videos, artists, and albums on YouTube Music. |
| `https://music.youtube.com/youtubei/v1/browse` | `POST` | `WEB_REMIX`, `ANDROID_MUSIC` (`6.42.52`) | Fetches artist pages (`UC...`), album tracks (`MPREb_...`), playlist contents (`VLPL...`), and user library playlists (`FEmusic_liked_playlists`, `FEmusic_library_landing`, `FEmusic_library_privately_owned_landing`). |
| `https://www.youtube.com/youtubei/v1/browse` | `POST` | `WEB` (`2.20240101.00.00`), `ANDROID` (`19.09.37`) | Fallback browse endpoint for public/unlisted playlists and `FEplaylist_aggregation`. |
| `https://www.youtube.com/youtubei/v1/player` | `POST` | `ANDROID_VR` (`1.56.21`), `IOS` (`19.29.1`), `ANDROID` (`19.09.37`), `TVHTML5_SIMPLY_EMBEDDED_PLAYER` | Resolves playable direct audio stream URLs (`audio/mp4` M4A / `audio/webm` Opus) from `streamingData.adaptiveFormats` for a given `videoId`. |
| `https://music.youtube.com/youtubei/v1/player` | `POST` | `ANDROID_MUSIC` | Resolves track metadata and audio streams from YouTube Music player endpoint. |
| `https://music.youtube.com/youtubei/v1/next` | `POST` | `WEB_REMIX` | Fallback playlist track fetcher using `playlistId` (`RD...`, `PL...`, `OLAK...`). |
| `https://music.youtube.com/youtubei/v1/account/account_menu` | `POST` | `WEB_REMIX`, `ANDROID_MUSIC` | Fetches signed-in user's display name (`accountName`), handle (`channelHandle`), and profile photo (`accountPhoto`). |
| `https://www.youtube.com/youtubei/v1/account/account_menu` | `POST` | `WEB`, `ANDROID` | Fallback endpoint for signed-in Google/YouTube account profile name and avatar. |
| `https://music.youtube.com/youtubei/v1/guide` | `POST` | `WEB_REMIX` | Secondary fallback for discovering user playlists listed in the navigation guide. |
| `https://music.youtube.com/youtubei/v1/playlist/create` | `POST` | `WEB_REMIX` | **Two-Way Sync:** Creates a new playlist on the user's YouTube Music account with `title`, `privacyStatus`, and initial `videoIds`. |
| `https://music.youtube.com/youtubei/v1/browse/edit_playlist` | `POST` | `WEB_REMIX` | **Two-Way Sync:** Edits an existing YouTube Music playlist (`ACTION_SET_PLAYLIST_NAME` for renaming, `ACTION_ADD_VIDEO` for adding songs, `ACTION_REMOVE_VIDEO` with `setVideoId` for removing songs). |
| `https://music.youtube.com/youtubei/v1/playlist/delete` | `POST` | `WEB_REMIX` | **Two-Way Sync:** Deletes a synced playlist (`playlistId`) from the user's YouTube Music library. |

### 3.2. Google & YouTube Web / OAuth / Data APIs (in `YouTubeRepository.kt` & `YouTubeLoginDialog.kt`)
| URL / Endpoint | Method | Purpose |
| :--- | :--- | :--- |
| `https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&hl=en&continue=https%3A%2F%2Fmusic.youtube.com%2F` | `GET` (WebView) | Interactive Google Sign-In flow inside `YouTubeLoginDialog` that redirects to `https://music.youtube.com` upon authentication. |
| `https://music.youtube.com` | `GET` | Cookie domain & HTML scraping fallback (`ytcfg.set`, `accountName`, `accountPhoto`) for authenticated user profile info. |
| `https://www.googleapis.com/oauth2/v3/userinfo` | `GET` | Fetches Google profile `name`, `given_name`, `family_name`, `picture`, and `email` when an OAuth2 Bearer token is present. |
| `https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true` | `GET` | Fetches the user's YouTube channel title, custom handle, and avatar via YouTube Data API v3. |
| `https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&mine=true&maxResults=50` | `GET` | Lists the user's own YouTube playlists via YouTube Data API v3 when Bearer auth is used. |
| `https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&maxResults=50&playlistId=<ID>` | `GET` | Lists tracks inside a YouTube playlist via YouTube Data API v3. |

### 3.3. Synchronized Lyrics API — LRCLib (in `LyricsRepository.kt`)
| Endpoint URL | Method | Purpose |
| :--- | :--- | :--- |
| `https://lrclib.net/api/get?track_name=<title>&artist_name=<artist>&duration=<sec>` | `GET` | Primary lookup for time-synced `.lrc` lyrics (`syncedLyrics`), plain lyrics (`plainLyrics`), or instrumental flag (`instrumental`). |
| `https://lrclib.net/api/get?track_name=<title>&artist_name=<artist>` | `GET` | Secondary fallback lookup without duration constraint (handles YouTube video intro/outro length differences). |
| `https://lrclib.net/api/search?track_name=<title>&artist_name=<artist>` | `GET` | Full-text search fallback returning a JSON array of matching lyric entries. |

### 3.4. In-App OTA Update API — GitHub Releases (in `UpdateManager.kt`)
| Endpoint URL | Method | Purpose |
| :--- | :--- | :--- |
| `https://api.github.com/repos/wallyxp/musesick-music-player/releases/latest` | `GET` | Checks the latest GitHub release tag (`tag_name`), changelog (`body`), and `.apk` asset download URL (`browser_download_url`). |
| `https://github.com/wallyxp/musesick-music-player/releases/download/.../*.apk` | `GET` | Streams the release APK (following 302/307 redirects to AWS S3) into `context.cacheDir/Musesick_update.apk` and triggers `FileProvider` package installation. |

### 3.5. Thumbnail & Artwork CDNs (in `YouTubeRepository.kt` & `MusesickApplication.kt`)
* `https://lh3.googleusercontent.com/...` & `https://yt3.googleusercontent.com/...` & `https://yt3.ggpht.com/...`: Google/YouTube artist and album artwork CDNs (dynamically rewritten via regex in `YouTubeRepository` to `=w120-h120-p-l90-rj` for fast list thumbnails or `=w800-h800-p-l90-rj` for high-res full player art).
* `https://i.ytimg.com/vi/<videoId>/hqdefault.jpg` & `maxresdefault.jpg`: Video/song thumbnail CDN.

---

## 4. Detailed File-by-File Breakdown

### 4.1. Root & Build Configuration Files
* **[`settings.gradle.kts`](file:///Users/admin/Desktop/musesick-music-player/settings.gradle.kts)**: Configures Gradle plugin repositories (`google()`, `mavenCentral()`, `gradlePluginPortal()`), sets `rootProject.name = "Musesick"`, and includes the `:app` module.
* **[`build.gradle.kts`](file:///Users/admin/Desktop/musesick-music-player/build.gradle.kts)**: Top-level Gradle build script declaring the Android Application plugin, Kotlin Android plugin, and Compose Compiler plugin versions.
* **[`gradle.properties`](file:///Users/admin/Desktop/musesick-music-player/gradle.properties)**: Configures JVM heap memory args (`org.gradle.jvmargs`), AndroidX flags (`android.useAndroidX=true`), and Kotlin code style.
* **[`app/build.gradle.kts`](file:///Users/admin/Desktop/musesick-music-player/app/build.gradle.kts)**: Module-level Gradle configuration defining namespace `com.wally.musesick`, `compileSdk = 35`, `minSdk = 26`, `versionName = "1.1.4"`, Java 21 compatibility, Jetpack Compose features, and all library dependencies.
* **[`app/proguard-rules.pro`](file:///Users/admin/Desktop/musesick-music-player/app/proguard-rules.pro)**: ProGuard/R8 shrinking and obfuscation rules for release builds.
* **[`app/src/main/AndroidManifest.xml`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/AndroidManifest.xml)**:
  * Declares permissions: `INTERNET`, `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, and `REQUEST_INSTALL_PACKAGES`.
  * Registers `MusesickApplication`, `MainActivity` (with `singleTop` launch mode and audio file `VIEW` intent filters), `MusicService` (with `MediaBrowserService` and `MEDIA_BUTTON` intent filters), and `androidx.core.content.FileProvider` for installing downloaded update APKs.

---

### 4.2. Application Entry Points (`com.wally.musesick`)
* **[`MusesickApplication.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/MusesickApplication.kt)**:
  * Custom `Application` class implementing Coil's `ImageLoaderFactory`.
  * Configures a global singleton `ImageLoader` with a **25% RAM `MemoryCache`** and a **150 MB `DiskCache`** (`image_cache` directory) with `CachePolicy.ENABLED` so artist avatars, album covers, and playlist thumbnails load instantaneously even offline.
* **[`MainActivity.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/MainActivity.kt)**:
  * Single-activity host (`ComponentActivity`) using `enableEdgeToEdge()`.
  * Requests runtime permissions (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` and `POST_NOTIFICATIONS` on Android 13+).
  * Handles incoming external audio `Intent.ACTION_VIEW` URIs (so opening an MP3/FLAC file from a file manager plays it directly in Musesick).
  * Intercepts system Back navigation via `BackHandler` and delegates to `MusicViewModel.navigateBack()`.
  * Hosts `MusesickTheme` and `MainScreen`.

---

### 4.3. Data Models (`com.wally.musesick.model`)
* **[`Track.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/model/Track.kt)**:
  * `enum class AudioFormat`: Defines `FLAC` (Lossless), `ALAC` (Apple Lossless), `WAV` (Uncompressed PCM), `M4A` (AAC), `MP3`, `OGG`, `OPUS`, `YOUTUBE_OPUS`, and `UNKNOWN`, with display badges and bitrates.
  * `data class Track`: Core song model (`id`, `title`, `artist`, `album`, `durationMs`, `thumbnailUrl`, `localUri`, `isLocal`, `audioFormat`, `bitrateKbps`, `sampleRateHz`, `isVideo`) plus `formattedDuration` (`mm:ss`) and `qualitySummary`.
  * `data class Artist`: Represents an artist (`id`, `name`, `thumbnailUrl`, `subtitle`, `browseId`, `isVisible`).
  * `data class Album`: Represents an album/EP (`id`, `title`, `artist`, `thumbnailUrl`, `browseId`, `year`).
  * `data class ArtistDetailData`: Holds an artist's `albums`, top `songs`, and `allSongsBrowseId`/`allSongsParams`.
  * `data class SearchResult`: Aggregates categorized search results (`artists`, `albums`, `songs`, `videos`).
* **[`Playlist.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/model/Playlist.kt)**:
  * `data class Playlist`: Represents a local or YouTube-synced playlist (`id`, `title`, `imageUri`, `tracks`, `createdAt`). Playlists synced with YouTube Music use the ID prefix `yt_sync_<YouTubePlaylistId>`.
  * `data class YouTubePlaylistData`: Represents a remote YouTube Music playlist (`id`, `title`, `author`, `thumbnailUrl`, `tracks`).
* **[`PlaybackState.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/model/PlaybackState.kt)**:
  * `enum class RepeatMode`: `OFF`, `ALL`, `ONE`.
  * `data class PlaybackState`: Immutable snapshot of the player (`currentTrack`, `isPlaying`, `isLoading`, `currentPositionMs`, `durationMs`, `isShuffleEnabled`, `repeatMode`, `error`) and computed `progress` (`0f..1f`).
* **[`LyricLine.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/model/LyricLine.kt)**:
  * `data class LyricLine(val time: Long, val text: String)`: A single timestamped lyric line in milliseconds.
* **[`LyricsUiState.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/model/LyricsUiState.kt)**:
  * `sealed class LyricsUiState`: `Idle`, `Loading`, `Success(lyrics, plainLyrics)`, `Instrumental`, and `Empty(message)`.
* **[`AppTheme.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/model/AppTheme.kt)**:
  * `enum class AppTheme`: Defines all supported app & player themes (`AMBIENT`, `DARK`, `LIGHT`, `AMOLED`, `MOCHA`, `OCEAN`, `FOREST`, `SAKURA`, `CUSTOM_COLOR`, `CUSTOM_IMAGE`) and their sub-variants (`ThemeVariant`).
* **[`PlayerStyle.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/model/PlayerStyle.kt)**:
  * `enum class PlayerStyle`: Defines the 5 selectable Now Playing screen styles:
    1. `FULLSCREEN_ALBUM_ART` (Immersive full-bleed cover art with gradient overlay)
    2. `NOTHING_GLYPH` (Nothing OS dot-matrix typography & animated Glyph visualizer)
    3. `CLASSIC_CARD` (Clean modern card layout)
    4. `MINIMAL_VINYL` (Spinning vinyl record turntable animation)
    5. `RETRO_ZUNE` (Microsoft Zune HD bold editorial typography style)
* **[`ThemePalettes.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/model/ThemePalettes.kt)**:
  * Defines `ColorPreset` and `AccentColorPresets` (curated accent colors for custom theming) and color-scheme builders for each `AppTheme` and `ThemeVariant`.

---

### 4.4. Audio Playback Engine & Foreground Service (`com.wally.musesick.player`)
* **[`PlayerManager.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/player/PlayerManager.kt)**:
  * Singleton controller managing `android.media.MediaPlayer`, queue state (`_queue`), shuffle/repeat logic, and a 250ms coroutine progress ticker (`startProgressTicker()`).
  * For **local tracks**, plays directly from the `content://` or `file://` URI via `MediaPlayer.setDataSource(context, uri)`.
  * For **YouTube streaming tracks**, resolves the direct audio stream URL on `Dispatchers.IO` via `YouTubeRepository.extractStreamUrl(track.id)` (caching resolved URLs in `streamUrlCache` with a 20-minute TTL and pre-fetching the next track's stream URL in the background via `prefetchNextTrackStream()`).
  * Starts and communicates with `MusicService` to keep the notification and `MediaSessionCompat` synchronized.
* **[`MusicService.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/player/MusicService.kt)**:
  * Foreground `Service` (`FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`) hosting `MediaSessionCompat` (`"MusesickMediaSession"`).
  * Builds and updates the system `MediaStyle` notification with album artwork (loaded via Coil), Play/Pause, Previous, Next, and Seekbar support on the Android lock screen and notification shade.
  * Handles hardware media buttons (`MediaButtonReceiver`) and notification action intents (`ACTION_PLAY`, `ACTION_PAUSE`, `ACTION_NEXT`, `ACTION_PREV`, `ACTION_STOP`).

---

### 4.5. Data Repositories (`com.wally.musesick.repository`)
* **[`YouTubeRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/YouTubeRepository.kt)**:
  * The core networking engine (~2,300 lines) interacting with YouTube & YouTube Music's InnerTube APIs.
  * **Search & Discovery**: `searchAll()`, `searchTracks()`, `searchArtists()`, `getArtistDetails()`, `fetchArtistAllTracks()`, `getAlbumDetails()`, `fetchTopSongsForArtists()`, `fetchRecentReleasesForArtists()`.
  * **Audio Stream Extraction**: `extractStreamUrl(videoId)` queries `ANDROID_VR`, `IOS`, `ANDROID`, and `TVHTML5` player clients and selects the highest-bitrate audio-only stream (`audio/mp4` or `audio/webm`).
  * **Account & Library Sync**: `fetchAccountInfo()` (extracts user's full name, handle, and profile picture across InnerTube `account_menu`, OAuth `userinfo`, YouTube v3 `channels`, and `music.youtube.com` HTML config) and `fetchUserLibraryPlaylists()` (fetches all user playlists and tracks).
  * **Two-Way Playlist Write Operations**:
    * `resolveVideoIdForTrack(track)`: Resolves a 11-character YouTube `videoId` for any track.
    * `createYouTubePlaylist(title, videoIds, privacyStatus)`: Creates a playlist on YouTube Music (`/youtubei/v1/playlist/create`).
    * `renameYouTubePlaylist(playlistId, newTitle)`: Renames a playlist on YouTube Music (`ACTION_SET_PLAYLIST_NAME`).
    * `addVideosToYouTubePlaylist(playlistId, videoIds)`: Adds songs to a YouTube Music playlist (`ACTION_ADD_VIDEO`).
    * `removeVideoFromYouTubePlaylist(playlistId, videoId)`: Looks up the playlist item's `setVideoId` and removes the song from YouTube Music (`ACTION_REMOVE_VIDEO`).
    * `deleteYouTubePlaylist(playlistId)`: Deletes a playlist from YouTube Music (`/youtubei/v1/playlist/delete`).
  * **Cryptographic Auth**: `buildSapisidAuthorization(sapisid, origin)` computes the `SAPISIDHASH` SHA-1 signature required by YouTube Music's authenticated endpoints.
* **[`PlaylistRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/PlaylistRepository.kt)**:
  * Manages local JSON persistence of all playlists in `SharedPreferences("musesick_playlists")`.
  * Supports CRUD operations (`createPlaylist`, `createPlaylistWithTracks`, `addTrackToPlaylist`, `removeTrackFromPlaylist`, `reorderTracksInPlaylist`, `updatePlaylist`, `deletePlaylist`), cover image copying/downloading into internal storage (`playlist_covers/`), JSON import/export (`exportPlaylistToJson`, `importPlaylistFromJson`), `replacePlaylistId(oldId, newId)` (used when linking an existing local playlist to a newly created `yt_sync_<id>` YouTube Music playlist), and `syncYouTubePlaylists()` (two-way merge of local and remote tracks).
* **[`LocalAudioRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/LocalAudioRepository.kt)**:
  * Queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` for local audio files (`>= 10s`) and parses SAF `content://` URIs picked by the user.
  * Determines `AudioFormat` (`FLAC`, `WAV`, `ALAC`, `M4A`, `MP3`, `OGG`) from MIME type and file extension, and extracts bitrate and sample rate via `MediaMetadataRetriever`.
* **[`LyricsRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/LyricsRepository.kt)**:
  * Queries `https://lrclib.net/api/get` and `https://lrclib.net/api/search` with cleaned track titles (`cleanTrackTitle`) and artist names (`cleanArtistName`), returning parsed `LyricLine` lists via `LrcParser`.
* **[`SettingsRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/SettingsRepository.kt)**:
  * Persists user preferences in `SharedPreferences("musesick_settings")`:
    * YouTube Music session cookie (`yt_music_cookie`), user name (`yt_user_name`), handle (`yt_user_handle`), and avatar URL (`yt_user_avatar_url`).
    * `AutoSyncInterval` (`DAILY`, `WEEKLY`, `MONTHLY`, `MANUAL` / `"When I Choose to Sync"`) and `lastPlaylistSyncTime`.
    * `PlayerStyle`, `AppTheme`, `appThemeVariant`, `playerTheme`, `customAccentColor`, `customThemeImagePath`, and onboarding completion flag.
* **[`SuggestedPlaylistsRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/SuggestedPlaylistsRepository.kt)**:
  * Generates and caches 3 personalized "For You" playlists (`Favorites Mix`, `New Releases`, `Most Listened`) based on the user's favorite artists and local `PlayCountRepository` statistics, refreshing every 3 days or when favorite artists change.
* **[`FavoriteArtistsRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/FavoriteArtistsRepository.kt)**:
  * Persists the user's favorite artists (`musesick_favorite_artists_prefs`) selected during onboarding or from the For You tab.
* **[`ArtistRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/ArtistRepository.kt)**:
  * Persists the customizable artist grid on the Stream tab (`musesick_artists_prefs`), defaulting to `YouTubeRepository.DEFAULT_INITIAL_ARTISTS`.
* **[`PlayCountRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/PlayCountRepository.kt)**:
  * Tracks how many times each song has been played (`musesick_play_counts_prefs`) to power the "Most Listened" smart playlist.
* **[`RecentlyPlayedRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/RecentlyPlayedRepository.kt)**:
  * Stores the last 50 played tracks in chronological order (`musesick_recently_played_prefs`).
* **[`SearchHistoryRepository.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/repository/SearchHistoryRepository.kt)**:
  * Stores the last 20 search queries (`musesick_search_history_prefs`).

---

### 4.6. ViewModel Layer (`com.wally.musesick.ui`)
* **[`MusicViewModel.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/MusicViewModel.kt)**:
  * Central state holder (`AndroidViewModel`) connecting the UI to all repositories and `PlayerManager`.
  * Defines `enum class ScreenState` (`HOME`, `ARTIST_DETAIL`, `ALBUM_DETAIL`, `PLAYLIST_DETAIL`, `ARTIST_MANAGER`, `ARTIST_TRACK_LIST`, `SEARCH`, `RECENTLY_PLAYED`, `SETTINGS`, `SETTINGS_NOW_PLAYING`, `SETTINGS_APP_THEME`, `SETTINGS_AUTO_SYNC`).
  * Coordinates **Two-Way YouTube Music Playlist Syncing**:
    * On startup (`init`), checks `autoSyncInterval` (`Daily`, `Weekly`, `Monthly`, `When I Choose to Sync`) against `lastPlaylistSyncTime` and triggers `refreshYtAccountAndPlaylists(silent = true)` when due.
    * In `refreshYtAccountAndPlaylists()`, uploads any existing local unsynced playlists (`!playlist.id.startsWith("yt_sync_")`) to YouTube Music via `pushLocalPlaylistToYouTube()`, converts their IDs to `yt_sync_<createdId>`, pushes local track additions/renames on already-synced playlists, and merges remote playlists.
    * Hooks `createPlaylist`, `addTrackToPlaylist`, `removeTrackFromPlaylist`, `editPlaylist`, `deletePlaylist`, `importPlaylistFromJson`, and `confirmImportYouTubePlaylist` so every playlist creation or edit is immediately reflected on YouTube Music.

---

### 4.7. UI Screens (`com.wally.musesick.ui.screens`)
* **[`MainScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/MainScreen.kt)**:
  * Top-level scaffold containing the Top Bar (Musesick branding, Search icon, and the Profile Avatar button that displays the default profile icon for `"Guest"` or the user's Google profile picture when signed in), the 3-tab `HorizontalPager` (`For You`, `Playlist`, `Local`), screen router (`AnimatedContent` across all `ScreenState`s), `MiniPlayerBar`, `FullPlayerSheet`, and modal dialogs (`YouTubeLoginDialog`, `UpdateDialog`, `SongActionMenuSheet`, `PlaylistDialogs`).
* **[`SettingsScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/SettingsScreen.kt)**:
  * Contains 4 composable screens:
    1. `SettingsScreen`: Displays the user's profile picture and `"Guest"` (or Google account name) in the header, **Check for Updates**, **Login with YouTube Music**, **Automatic Sync**, **Now Playing**, and **App Theme**.
    2. `SettingsAutoSyncScreen`: Dedicated window with the 4 sync frequency options (**Daily**, **Weekly**, **Monthly**, **When I Choose to Sync**) and a bottom-centered **"Sync Now"** button.
    3. `SettingsNowPlayingScreen`: Lets the user switch between the 5 `PlayerStyle` layouts and customize the player's theme override.
    4. `SettingsAppThemeScreen`: Lets the user choose `AppTheme`, sub-variants, custom accent colors, or custom background images.
* **[`ForYouTab.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/ForYouTab.kt)**:
  * Home tab featuring **Recently Played**, **Suggested For You** smart playlists (`Favorites Mix`, `New Releases`, `Most Listened`), and **Favorite Artists** horizontal carousel.
* **[`PlaylistsTab.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/PlaylistsTab.kt)**:
  * Displays the grid/list of user & synced YouTube Music playlists with options to create a new playlist, import from YouTube URL, or import from a `.json` backup file.
* **[`PlaylistDetailScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/PlaylistDetailScreen.kt)**:
  * Displays playlist cover art, title, track count, **Play All** / **Shuffle** buttons, drag-to-reorder track list, track removal, playlist rename/cover editing, and JSON export/share.
* **[`LocalTab.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/LocalTab.kt)**:
  * Displays device audio files with format filter chips (`ALL`, `FLAC`, `M4A`, `MP3`), quality badges, and a folder/file picker button.
* **[`StreamTab.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/StreamTab.kt)**:
  * Displays Zune-inspired artist tiles for quick streaming browsing.
* **[`FullPlayerSheet.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/FullPlayerSheet.kt)**:
  * Full-screen modal player supporting all 5 `PlayerStyle` designs, interactive queue sheet (with drag-to-reorder), synchronized lyrics overlay (`LyricsView`), sleep timer, and audio format indicator.
* **[`SearchScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/SearchScreen.kt)**:
  * Unified YouTube Music search screen with recent search history chips and categorized tabs (`Songs`, `Artists`, `Albums`, `Videos`).
* **[`ArtistDetailScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/ArtistDetailScreen.kt)** & **[`ArtistTrackListScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/ArtistTrackListScreen.kt)**:
  * Displays an artist's hero image, favorite toggle, top songs, discography (albums/EPs), and full song catalog.
* **[`AlbumDetailScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/AlbumDetailScreen.kt)**:
  * Displays album artwork, release year, and tracklist with Play All / Shuffle controls.
* **[`ArtistOnboardingScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/ArtistOnboardingScreen.kt)**, **[`ArtistSearchScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/ArtistSearchScreen.kt)**, & **[`ArtistManagerSheet.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/ArtistManagerSheet.kt)**:
  * First-launch onboarding screen to pick 5+ favorite artists, plus management screens for adding/removing artists.
* **[`RecentlyPlayedScreen.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/screens/RecentlyPlayedScreen.kt)**:
  * Full history view of recently played tracks with Play All, Shuffle, and Clear History actions.

---

### 4.8. Reusable UI Components (`com.wally.musesick.ui.components`)
* **[`YouTubeLoginDialog.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/YouTubeLoginDialog.kt)**:
  * Full-screen dialog hosting an Android `WebView` pointed at Google's `ServiceLogin` for YouTube Music.
  * Spoofs a clean Pixel Chrome Mobile User-Agent so Google allows passkeys and 2-Step Verification without WebView blocks.
  * Intercepts `CookieManager.getInstance().getCookie("https://music.youtube.com")`, detects when `SAPISID` / `__Secure-3PAPISID` authentication cookies appear, extracts the user's Google account name & avatar from `music.youtube.com`, and invokes `onLoginSuccess(cookie, accountName, avatarUrl)`.
* **[`MiniPlayerBar.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/MiniPlayerBar.kt)**:
  * Floating bottom mini-player bar showing current track artwork, scrolling title/artist, progress bar, Play/Pause, and Next track button.
* **[`LyricsView.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/LyricsView.kt)**:
  * Auto-scrolling synchronized karaoke-style lyrics view that highlights the active `LyricLine` based on `playbackState.currentPositionMs` and allows tapping any lyric line to seek to that timestamp.
* **[`PlaylistDialogs.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/PlaylistDialogs.kt)**:
  * Bottom sheets and dialogs for creating a new playlist (with optional cover image or YouTube playlist link import), adding a song to an existing playlist, and editing a playlist's name/artwork.
* **[`SongActionMenuSheet.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/SongActionMenuSheet.kt)**:
  * Long-press context menu sheet for any song (`Play Now`, `Play Next`, `Add to Queue`, `Add to Playlist`, `Create New Playlist`).
* **[`UpdateDialog.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/UpdateDialog.kt)**:
  * Modal dialog showing new GitHub release version, APK size, release notes, download progress bar, and one-tap install trigger.
* **[`DotMatrixText.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/DotMatrixText.kt)**, **[`NothingButtons.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/NothingButtons.kt)**, **[`NothingSlider.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/NothingSlider.kt)**, **[`VisualizerGlyph.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/VisualizerGlyph.kt)**, & **[`ZuneArtistTile.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/ZuneArtistTile.kt)**:
  * Custom Canvas-drawn components powering the Nothing OS dot-matrix typography, animated audio equalizer bars, custom seekbars, and Zune editorial tiles.
* **[`AddFavoriteArtistSheet.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/components/AddFavoriteArtistSheet.kt)**:
  * Bottom sheet for searching and adding favorite artists from the For You tab.

---

### 4.9. Theme System, Utilities & Updater
* **[`AmbientTheme.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/theme/AmbientTheme.kt)**, **[`Theme.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/theme/Theme.kt)**, **[`Color.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/theme/Color.kt)**, & **[`Type.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/ui/theme/Type.kt)**:
  * Dynamic Compose `MaterialTheme` wrapper (`MusesickTheme`) that computes ambient gradient brushes (`rememberAmbientBrush`) and applies user-selected color schemes (`AMOLED`, `MOCHA`, `OCEAN`, `FOREST`, `SAKURA`, `CUSTOM_COLOR`, `CUSTOM_IMAGE`).
* **[`LrcParser.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/util/LrcParser.kt)**:
  * Regex parser that converts standard `[mm:ss.xx]` or `[mm:ss.xxx]` `.lrc` text into sorted `List<LyricLine>` objects.
* **[`UpdateManager.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/java/com/wally/musesick/update/UpdateManager.kt)**:
  * Handles semantic version comparison (`isNewerVersion`), GitHub Release API polling, streaming APK downloads with live progress callbacks, and `FileProvider` APK installation (`application/vnd.android.package-archive`).

---

### 4.10. Android Resources & Unit Tests
* **[`app/src/main/res/xml/file_paths.xml`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/res/xml/file_paths.xml)**: Grants `FileProvider` access to `cache-path` (`Musesick_update.apk`) and `files-path` for OTA updates and file sharing.
* **[`app/src/main/res/drawable/ic_notification.xml`](file:///Users/admin/Desktop/musesick-music-player/app/src/main/res/drawable/ic_notification.xml)**: Vector icon displayed in the Android status bar during media playback.
* **Unit Tests (`app/src/test/java/com/wally/musesick/`)**:
  * [`LrcParserTest.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/test/java/com/wally/musesick/LrcParserTest.kt): Verifies LRC timestamp parsing and sorting.
  * [`LyricsRepositoryTest.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/test/java/com/wally/musesick/LyricsRepositoryTest.kt): Verifies track title and artist name cleaning regexes.
  * [`PlaylistTest.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/test/java/com/wally/musesick/PlaylistTest.kt): Verifies playlist model behavior and JSON serialization.
  * [`SettingsModelsTest.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/test/java/com/wally/musesick/SettingsModelsTest.kt): Verifies `AppTheme` and `PlayerStyle` enum serialization and fallbacks.
  * [`YouTubeRepositoryTest.kt`](file:///Users/admin/Desktop/musesick-music-player/app/src/test/java/com/wally/musesick/YouTubeRepositoryTest.kt): Verifies YouTube playlist URL/ID extraction and thumbnail URL upgrading.
