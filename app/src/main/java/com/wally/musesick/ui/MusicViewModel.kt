package com.wally.musesick.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wally.musesick.update.AppUpdateInfo
import com.wally.musesick.update.UpdateManager
import androidx.compose.ui.graphics.Color
import com.wally.musesick.model.AppTheme
import com.wally.musesick.model.Album
import com.wally.musesick.model.Artist
import com.wally.musesick.model.ArtistDetailData
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.PlaybackState
import com.wally.musesick.model.SearchResult
import com.wally.musesick.model.Track
import com.wally.musesick.player.PlayerManager
import com.wally.musesick.repository.ArtistRepository
import com.wally.musesick.repository.LocalAudioRepository
import com.wally.musesick.repository.YouTubeRepository
import com.wally.musesick.model.Playlist
import com.wally.musesick.model.PlayerStyle
import com.wally.musesick.model.YouTubePlaylistData
import com.wally.musesick.repository.PlaylistRepository
import com.wally.musesick.repository.FavoriteArtistsRepository
import com.wally.musesick.repository.RecentlyPlayedRepository
import com.wally.musesick.repository.SearchHistoryRepository
import com.wally.musesick.repository.SettingsRepository
import com.wally.musesick.repository.PlayCountRepository
import com.wally.musesick.repository.SuggestedPlaylistsRepository
import com.wally.musesick.model.LyricsUiState
import com.wally.musesick.repository.LyricsRepository
import com.wally.musesick.repository.LyricsResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    ARTIST_DETAIL,
    ALBUM_DETAIL,
    PLAYLIST_DETAIL,
    ARTIST_MANAGER,
    ARTIST_TRACK_LIST,
    SEARCH,
    RECENTLY_PLAYED,
    SETTINGS,
    SETTINGS_NOW_PLAYING,
    SETTINGS_APP_THEME,
    SETTINGS_AUTO_SYNC
}

enum class ArtistTrackListType {
    SONGS
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val ytRepository = YouTubeRepository()
    private val localRepository = LocalAudioRepository(application)
    private val playlistRepository = PlaylistRepository(application)
    private val artistRepository = ArtistRepository(application)
    private val recentlyPlayedRepository = RecentlyPlayedRepository(application)
    private val favoriteArtistsRepository = FavoriteArtistsRepository(application)
    private val searchHistoryRepository = SearchHistoryRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val playCountRepository = PlayCountRepository(application)
    private val suggestedPlaylistsRepository = SuggestedPlaylistsRepository(application, ytRepository, playCountRepository)
    private val lyricsRepository = LyricsRepository()
    private val updateManager = UpdateManager()
    val playerManager = PlayerManager.getInstance(application)

    // Lyrics State
    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()
    private val lyricsCache = mutableMapOf<String, LyricsUiState>()
    private var lastLyricsTrackId: String? = null

    // Onboarding & Preferences
    private val _hasCompletedArtistOnboarding = MutableStateFlow(settingsRepository.hasCompletedArtistOnboarding())
    val hasCompletedArtistOnboarding: StateFlow<Boolean> = _hasCompletedArtistOnboarding.asStateFlow()

    private val _playerStyle = MutableStateFlow(settingsRepository.getPlayerStyle())
    val playerStyle: StateFlow<PlayerStyle> = _playerStyle.asStateFlow()

    private val _appTheme = MutableStateFlow(settingsRepository.getAppTheme())
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _appThemeVariant = MutableStateFlow(
        settingsRepository.getAppThemeVariant().ifBlank { _appTheme.value.defaultVariant }
    )
    val appThemeVariant: StateFlow<String> = _appThemeVariant.asStateFlow()

    private val _playerTheme = MutableStateFlow(settingsRepository.getPlayerTheme())
    val playerTheme: StateFlow<AppTheme?> = _playerTheme.asStateFlow()

    private val _playerThemeVariant = MutableStateFlow(
        settingsRepository.getPlayerThemeVariant() ?: _playerTheme.value?.defaultVariant
    )
    val playerThemeVariant: StateFlow<String?> = _playerThemeVariant.asStateFlow()

    private val _customAccentColor = MutableStateFlow(Color(settingsRepository.getCustomAccentColor()))
    val customAccentColor: StateFlow<Color> = _customAccentColor.asStateFlow()

    private val _customThemeImagePath = MutableStateFlow(settingsRepository.getCustomThemeImagePath())
    val customThemeImagePath: StateFlow<String?> = _customThemeImagePath.asStateFlow()

    private val _isSettingsDialogOpen = MutableStateFlow(false)
    val isSettingsDialogOpen: StateFlow<Boolean> = _isSettingsDialogOpen.asStateFlow()

    // Suggested Playlists State
    private val _suggestedPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val suggestedPlaylists: StateFlow<List<Playlist>> = _suggestedPlaylists.asStateFlow()

    private val _isSuggestedPlaylistsLoading = MutableStateFlow(false)
    val isSuggestedPlaylistsLoading: StateFlow<Boolean> = _isSuggestedPlaylistsLoading.asStateFlow()

    // Recently Played State
    private val _recentlyPlayed = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = _recentlyPlayed.asStateFlow()

    // Favorite Artists State (Initially empty)
    private val _favoriteArtists = MutableStateFlow<List<Artist>>(emptyList())
    val favoriteArtists: StateFlow<List<Artist>> = _favoriteArtists.asStateFlow()

    // Recent Searches State (Last 20)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // In-App Update State
    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _updateToastMessage = MutableStateFlow<String?>(null)
    val updateToastMessage: StateFlow<String?> = _updateToastMessage.asStateFlow()

    val currentAppVersion: String
        get() = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val queue: StateFlow<List<Track>> = playerManager.queue

    private val _isQueueReorderable = MutableStateFlow(true)
    val isQueueReorderable: StateFlow<Boolean> = _isQueueReorderable.asStateFlow()

    // Navigation Stack
    private val _currentScreen = MutableStateFlow(ScreenState.HOME)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0 = STREAM, 1 = LOCAL, 2 = PLAYLISTS
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Playlists State
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    // YouTube Playlist Import State
    private val _isImportingYtPlaylist = MutableStateFlow(false)
    val isImportingYtPlaylist: StateFlow<Boolean> = _isImportingYtPlaylist.asStateFlow()

    private val _ytPlaylistPreview = MutableStateFlow<YouTubePlaylistData?>(null)
    val ytPlaylistPreview: StateFlow<YouTubePlaylistData?> = _ytPlaylistPreview.asStateFlow()

    private val _ytPlaylistImportError = MutableStateFlow<String?>(null)
    val ytPlaylistImportError: StateFlow<String?> = _ytPlaylistImportError.asStateFlow()

    // Song Context Menu State
    private val _contextMenuTrack = MutableStateFlow<Track?>(null)
    val contextMenuTrack: StateFlow<Track?> = _contextMenuTrack.asStateFlow()

    private val _targetTrackForPlaylist = MutableStateFlow<Track?>(null)
    val targetTrackForPlaylist: StateFlow<Track?> = _targetTrackForPlaylist.asStateFlow()

    private val _isExistingPlaylistSheetOpen = MutableStateFlow(false)
    val isExistingPlaylistSheetOpen: StateFlow<Boolean> = _isExistingPlaylistSheetOpen.asStateFlow()

    private val _isNewPlaylistSheetOpen = MutableStateFlow(false)
    val isNewPlaylistSheetOpen: StateFlow<Boolean> = _isNewPlaylistSheetOpen.asStateFlow()

    // Artists on Home Page
    private val _artists = MutableStateFlow(YouTubeRepository.DEFAULT_INITIAL_ARTISTS)
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _artistSearchQuery = MutableStateFlow("")
    val artistSearchQuery: StateFlow<String> = _artistSearchQuery.asStateFlow()

    private val _artistSearchResults = MutableStateFlow<List<Artist>>(emptyList())
    val artistSearchResults: StateFlow<List<Artist>> = _artistSearchResults.asStateFlow()

    private val _isArtistSearching = MutableStateFlow(false)
    val isArtistSearching: StateFlow<Boolean> = _isArtistSearching.asStateFlow()

    private var artistSearchJob: Job? = null

    // Artist Detail State
    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    private val _artistAlbums = MutableStateFlow<List<Album>>(emptyList())
    val artistAlbums: StateFlow<List<Album>> = _artistAlbums.asStateFlow()

    private val _artistSongs = MutableStateFlow<List<Track>>(emptyList())
    val artistSongs: StateFlow<List<Track>> = _artistSongs.asStateFlow()

    private var currentArtistDetailData: ArtistDetailData? = null

    private val _artistTrackListType = MutableStateFlow(ArtistTrackListType.SONGS)
    val artistTrackListType: StateFlow<ArtistTrackListType> = _artistTrackListType.asStateFlow()

    private val _allArtistTracks = MutableStateFlow<List<Track>>(emptyList())
    val allArtistTracks: StateFlow<List<Track>> = _allArtistTracks.asStateFlow()

    private val _isAllTracksLoading = MutableStateFlow(false)
    val isAllTracksLoading: StateFlow<Boolean> = _isAllTracksLoading.asStateFlow()

    private val _isArtistLoading = MutableStateFlow(false)
    val isArtistLoading: StateFlow<Boolean> = _isArtistLoading.asStateFlow()

    // Album Detail State
    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _albumTracks = MutableStateFlow<List<Track>>(emptyList())
    val albumTracks: StateFlow<List<Track>> = _albumTracks.asStateFlow()

    private val _isAlbumLoading = MutableStateFlow(false)
    val isAlbumLoading: StateFlow<Boolean> = _isAlbumLoading.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow(SearchResult())
    val searchResult: StateFlow<SearchResult> = _searchResult.asStateFlow()

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading: StateFlow<Boolean> = _isSearchLoading.asStateFlow()

    // Local Tracks State
    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    private val _isLocalLoading = MutableStateFlow(false)
    val isLocalLoading: StateFlow<Boolean> = _isLocalLoading.asStateFlow()

    private val _localFormatFilter = MutableStateFlow("ALL")
    val localFormatFilter: StateFlow<String> = _localFormatFilter.asStateFlow()

    private val _isFullPlayerOpen = MutableStateFlow(false)
    val isFullPlayerOpen: StateFlow<Boolean> = _isFullPlayerOpen.asStateFlow()

    // YouTube Music Account & Sync State
    private val _ytAccountInfo = MutableStateFlow<com.wally.musesick.repository.YtAccountInfo?>(
        settingsRepository.getYtUserName()?.let { name ->
            com.wally.musesick.repository.YtAccountInfo(
                name = name,
                handle = settingsRepository.getYtUserHandle() ?: "",
                avatarUrl = settingsRepository.getYtUserAvatarUrl()
            )
        }
    )
    val ytAccountInfo: StateFlow<com.wally.musesick.repository.YtAccountInfo?> = _ytAccountInfo.asStateFlow()

    private val _isSyncingYtAccount = MutableStateFlow(false)
    val isSyncingYtAccount: StateFlow<Boolean> = _isSyncingYtAccount.asStateFlow()

    private val _autoSyncInterval = MutableStateFlow(settingsRepository.getAutoSyncInterval())
    val autoSyncInterval: StateFlow<SettingsRepository.AutoSyncInterval> = _autoSyncInterval.asStateFlow()

    private val _lastPlaylistSyncTime = MutableStateFlow(settingsRepository.getLastPlaylistSyncTime())
    val lastPlaylistSyncTime: StateFlow<Long> = _lastPlaylistSyncTime.asStateFlow()

    private var searchJob: Job? = null

    init {
        ytRepository.cookie = settingsRepository.getYtMusicCookie()
        loadArtists()
        loadLocalTracks()
        loadPlaylists()
        loadRecentlyPlayed()
        loadFavoriteArtists()
        loadRecentSearches()
        observePlaybackForRecentHistory()
        observePlaybackForLyrics()
        if (!ytRepository.cookie.isNullOrBlank()) {
            val interval = settingsRepository.getAutoSyncInterval()
            val lastSync = settingsRepository.getLastPlaylistSyncTime()
            val now = System.currentTimeMillis()
            if (interval.durationMs != null && (lastSync == 0L || now - lastSync >= interval.durationMs)) {
                refreshYtAccountAndPlaylists(silent = true)
            } else {
                // Still refresh account metadata quietly if missing
                viewModelScope.launch {
                    val fetchedAccount = ytRepository.fetchAccountInfo(fallbackAccountName = settingsRepository.getYtUserName())
                    if (fetchedAccount != null) {
                        settingsRepository.setYtUserName(fetchedAccount.name)
                        settingsRepository.setYtUserHandle(fetchedAccount.handle)
                        settingsRepository.setYtUserAvatarUrl(fetchedAccount.avatarUrl)
                        _ytAccountInfo.value = fetchedAccount
                    }
                }
            }
        }
    }

    fun setAutoSyncInterval(interval: SettingsRepository.AutoSyncInterval) {
        settingsRepository.setAutoSyncInterval(interval)
        _autoSyncInterval.value = interval
        if (!ytRepository.cookie.isNullOrBlank() && interval.durationMs != null) {
            val lastSync = settingsRepository.getLastPlaylistSyncTime()
            val now = System.currentTimeMillis()
            if (lastSync == 0L || now - lastSync >= interval.durationMs) {
                refreshYtAccountAndPlaylists(silent = true)
            }
        }
    }

    fun onYtMusicLoginSuccess(cookie: String, fallbackName: String? = null, fallbackAvatarUrl: String? = null) {
        settingsRepository.setYtMusicCookie(cookie)
        ytRepository.cookie = cookie
        refreshYtAccountAndPlaylists(
            silent = false,
            fallbackName = fallbackName,
            fallbackAvatarUrl = fallbackAvatarUrl
        )
    }

    private suspend fun pushLocalPlaylistToYouTube(localPl: Playlist): String? {
        if (ytRepository.cookie.isNullOrBlank()) return null
        val videoIds = localPl.tracks.mapNotNull { track ->
            ytRepository.resolveVideoIdForTrack(track)
        }
        val createdYtId = ytRepository.createYouTubePlaylist(
            title = localPl.title,
            videoIds = videoIds
        )
        if (!createdYtId.isNullOrBlank()) {
            val newSyncedId = "yt_sync_$createdYtId"
            val updatedList = playlistRepository.replacePlaylistId(localPl.id, newSyncedId)
            _playlists.value = updatedList
            if (_selectedPlaylist.value?.id == localPl.id) {
                _selectedPlaylist.value = updatedList.find { it.id == newSyncedId }
            }
            return createdYtId
        }
        return null
    }

    fun refreshYtAccountAndPlaylists(
        silent: Boolean = false,
        fallbackName: String? = null,
        fallbackAvatarUrl: String? = null
    ) {
        if (ytRepository.cookie.isNullOrBlank()) {
            if (!silent) {
                _updateToastMessage.value = "Please log in to YouTube Music first to sync playlists"
            }
            return
        }
        viewModelScope.launch {
            _isSyncingYtAccount.value = true
            try {
                val fetchedAccount = ytRepository.fetchAccountInfo(fallbackAccountName = fallbackName)
                val savedName = settingsRepository.getYtUserName()

                val resolvedName = when {
                    !fetchedAccount?.name.isNullOrBlank() -> fetchedAccount!!.name
                    !fallbackName.isNullOrBlank() -> fallbackName
                    !savedName.isNullOrBlank() -> savedName
                    else -> "YouTube Music User"
                }
                val resolvedHandle = fetchedAccount?.handle
                    ?: settingsRepository.getYtUserHandle()
                    ?: ""
                val resolvedAvatar = fetchedAccount?.avatarUrl
                    ?: fallbackAvatarUrl?.takeIf { it.isNotBlank() }
                    ?: settingsRepository.getYtUserAvatarUrl()

                val info = com.wally.musesick.repository.YtAccountInfo(
                    name = resolvedName,
                    handle = resolvedHandle,
                    avatarUrl = resolvedAvatar
                )
                settingsRepository.setYtUserName(info.name)
                settingsRepository.setYtUserHandle(info.handle)
                settingsRepository.setYtUserAvatarUrl(info.avatarUrl)
                _ytAccountInfo.value = info

                // 1. Fetch existing remote playlists first
                val initialRemotePlaylists = ytRepository.fetchUserLibraryPlaylists().toMutableList()

                // 2. Push any unsynced local playlists (or local changes on synced playlists) to YouTube Music
                val currentLocalPlaylists = playlistRepository.getPlaylists()
                var uploadedCount = 0
                for (localPl in currentLocalPlaylists) {
                    if (!localPl.id.startsWith("yt_sync_")) {
                        // Check if a remote playlist with the same title already exists
                        val existingRemote = initialRemotePlaylists.find {
                            it.title.equals(localPl.title, ignoreCase = true)
                        }
                        if (existingRemote != null) {
                            val newSyncedId = "yt_sync_${existingRemote.id}"
                            val remoteTrackIds = existingRemote.tracks.map { it.id }.toSet()
                            val missingVideoIds = localPl.tracks
                                .mapNotNull { ytRepository.resolveVideoIdForTrack(it) }
                                .filter { it !in remoteTrackIds }
                            if (missingVideoIds.isNotEmpty()) {
                                ytRepository.addVideosToYouTubePlaylist(existingRemote.id, missingVideoIds)
                            }
                            val updatedList = playlistRepository.replacePlaylistId(localPl.id, newSyncedId)
                            _playlists.value = updatedList
                            if (_selectedPlaylist.value?.id == localPl.id) {
                                _selectedPlaylist.value = updatedList.find { it.id == newSyncedId }
                            }
                            uploadedCount++
                        } else {
                            // Create this local playlist on YouTube Music
                            val createdYtId = pushLocalPlaylistToYouTube(localPl)
                            if (createdYtId != null) {
                                uploadedCount++
                            }
                        }
                    } else {
                        // Already a yt_sync_ playlist: push any locally added tracks or title renames
                        val ytPlaylistId = localPl.id.removePrefix("yt_sync_")
                        val matchingRemote = initialRemotePlaylists.find { it.id == ytPlaylistId }
                        if (matchingRemote != null) {
                            if (localPl.title != matchingRemote.title && localPl.title.isNotBlank()) {
                                ytRepository.renameYouTubePlaylist(ytPlaylistId, localPl.title)
                            }
                            val remoteTrackIds = matchingRemote.tracks.map { it.id }.toSet()
                            val missingVideoIds = localPl.tracks
                                .mapNotNull { ytRepository.resolveVideoIdForTrack(it) }
                                .filter { it !in remoteTrackIds }
                            if (missingVideoIds.isNotEmpty()) {
                                ytRepository.addVideosToYouTubePlaylist(ytPlaylistId, missingVideoIds)
                            }
                        }
                    }
                }

                // 3. Re-fetch remote playlists if we pushed changes, then merge with local repository
                val finalRemotePlaylists = if (uploadedCount > 0) {
                    ytRepository.fetchUserLibraryPlaylists().ifEmpty { initialRemotePlaylists }
                } else {
                    initialRemotePlaylists
                }

                val updated = playlistRepository.syncYouTubePlaylists(finalRemotePlaylists)
                _playlists.value = updated
                if (_selectedPlaylist.value != null) {
                    _selectedPlaylist.value = updated.find { it.id == _selectedPlaylist.value?.id }
                }

                val syncNowMs = System.currentTimeMillis()
                settingsRepository.setLastPlaylistSyncTime(syncNowMs)
                _lastPlaylistSyncTime.value = syncNowMs

                val syncedTotal = updated.count { it.id.startsWith("yt_sync_") }
                if (!silent) {
                    _updateToastMessage.value = "Synced $syncedTotal playlists with YouTube Music"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!silent) {
                    _updateToastMessage.value = "Failed to sync some playlists with YouTube Music"
                }
            } finally {
                _isSyncingYtAccount.value = false
            }
        }
    }

    fun logoutYtMusic() {
        viewModelScope.launch {
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.CookieManager.getInstance().flush()
            } catch (_: Exception) {}
            settingsRepository.clearYtAccount()
            ytRepository.cookie = null
            ytRepository.accessToken = null
            ytRepository.profileAccessToken = null
            _ytAccountInfo.value = null
            val updated = playlistRepository.removeSyncedYouTubePlaylists()
            _playlists.value = updated
            _updateToastMessage.value = "Logged out of YouTube Music"
        }
    }

    private fun observePlaybackForRecentHistory() {
        viewModelScope.launch {
            playerManager.playbackState.collect { state ->
                val track = state.currentTrack
                if (track != null && _recentlyPlayed.value.firstOrNull()?.id != track.id) {
                    recordTrackPlayed(track)
                }
            }
        }
    }

    private fun observePlaybackForLyrics() {
        viewModelScope.launch {
            playerManager.playbackState.collect { state ->
                val track = state.currentTrack
                if (track != null && track.id != lastLyricsTrackId) {
                    lastLyricsTrackId = track.id
                    loadLyricsForTrack(track)
                }
            }
        }
    }

    fun loadLyricsForTrack(track: Track, forceRefresh: Boolean = false) {
        val cached = lyricsCache[track.id]
        if (!forceRefresh && cached != null) {
            _lyricsState.value = cached
            return
        }

        viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Loading
            val durationSec = track.durationMs / 1000L
            val result = lyricsRepository.fetchLyrics(
                trackName = track.title,
                artistName = track.artist,
                durationSeconds = durationSec
            )
            val uiState = when (result) {
                is LyricsResult.Success -> LyricsUiState.Success(
                    lyrics = result.lyrics,
                    plainLyrics = result.plainLyrics
                )
                is LyricsResult.Instrumental -> LyricsUiState.Instrumental
                is LyricsResult.NotFound -> LyricsUiState.Empty(result.reason)
                is LyricsResult.Error -> LyricsUiState.Empty("Lyrics not available")
            }
            lyricsCache[track.id] = uiState
            if (playerManager.playbackState.value.currentTrack?.id == track.id) {
                _lyricsState.value = uiState
            }
        }
    }

    fun loadArtists() {
        viewModelScope.launch {
            _artists.value = artistRepository.getArtists()
            fetchMissingArtistThumbnails()
        }
    }

    private fun fetchMissingArtistThumbnails() {
        viewModelScope.launch {
            val currentList = _artists.value
            for (artist in currentList) {
                if (artist.thumbnailUrl.isNullOrEmpty()) {
                    val (thumb, browseId) = ytRepository.fetchArtistInfo(artist.name)
                    if (!thumb.isNullOrEmpty() || !browseId.isNullOrEmpty()) {
                        _artists.value = _artists.value.map {
                            if (it.id == artist.id) {
                                it.copy(
                                    thumbnailUrl = thumb ?: it.thumbnailUrl,
                                    browseId = browseId ?: it.browseId
                                )
                            } else it
                        }
                    }
                }
            }
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
        _currentScreen.value = ScreenState.HOME
        if (index == 1 && _localTracks.value.isEmpty()) {
            loadLocalTracks()
        } else if (index == 2) {
            loadPlaylists()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResult.value = SearchResult()
            _isSearchLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isSearchLoading.value = true
            val results = ytRepository.searchAll(query)
            _searchResult.value = results
            _isSearchLoading.value = false
        }
    }

    fun selectArtist(artist: Artist) {
        _selectedArtist.value = artist
        _artistAlbums.value = emptyList()
        _artistSongs.value = emptyList()
        _allArtistTracks.value = emptyList()
        currentArtistDetailData = null
        _currentScreen.value = ScreenState.ARTIST_DETAIL

        viewModelScope.launch {
            _isArtistLoading.value = true
            val data = ytRepository.getArtistDetails(artist)
            currentArtistDetailData = data
            _artistAlbums.value = data.albums
            _artistSongs.value = data.songs
            _isArtistLoading.value = false
        }
    }

    fun openArtistSongsList() {
        val artist = _selectedArtist.value ?: return
        _artistTrackListType.value = ArtistTrackListType.SONGS
        _currentScreen.value = ScreenState.ARTIST_TRACK_LIST
        _allArtistTracks.value = _artistSongs.value

        viewModelScope.launch {
            _isAllTracksLoading.value = true
            val browseId = currentArtistDetailData?.allSongsBrowseId
            val params = currentArtistDetailData?.allSongsParams
            val all = ytRepository.fetchArtistAllTracks(
                browseId = browseId,
                params = params,
                fallbackQuery = "${artist.name} songs",
                artistName = artist.name,
                isVideo = false
            )
            if (all.isNotEmpty()) {
                _allArtistTracks.value = all
            }
            _isAllTracksLoading.value = false
        }
    }

    fun selectAlbum(album: Album) {
        _selectedAlbum.value = album
        _albumTracks.value = emptyList()
        _currentScreen.value = ScreenState.ALBUM_DETAIL

        viewModelScope.launch {
            _isAlbumLoading.value = true
            val tracks = ytRepository.getAlbumTracks(album)
            _albumTracks.value = tracks
            _isAlbumLoading.value = false
        }
    }

    fun navigateBack(): Boolean {
        return when (_currentScreen.value) {
            ScreenState.SEARCH -> {
                closeSearch()
                true
            }
            ScreenState.RECENTLY_PLAYED -> {
                _currentScreen.value = ScreenState.HOME
                true
            }
            ScreenState.ARTIST_TRACK_LIST -> {
                _currentScreen.value = ScreenState.ARTIST_DETAIL
                true
            }
            ScreenState.ALBUM_DETAIL -> {
                _currentScreen.value = if (_selectedArtist.value != null) ScreenState.ARTIST_DETAIL else ScreenState.HOME
                true
            }
            ScreenState.ARTIST_DETAIL -> {
                _currentScreen.value = ScreenState.HOME
                _selectedArtist.value = null
                true
            }
            ScreenState.PLAYLIST_DETAIL -> {
                _currentScreen.value = ScreenState.HOME
                _selectedPlaylist.value = null
                true
            }
            ScreenState.ARTIST_MANAGER -> {
                closeArtistManager()
                true
            }
            ScreenState.SETTINGS_NOW_PLAYING -> {
                _currentScreen.value = ScreenState.SETTINGS
                true
            }
            ScreenState.SETTINGS_APP_THEME -> {
                _currentScreen.value = ScreenState.SETTINGS
                true
            }
            ScreenState.SETTINGS_AUTO_SYNC -> {
                _currentScreen.value = ScreenState.SETTINGS
                true
            }
            ScreenState.SETTINGS -> {
                _currentScreen.value = ScreenState.HOME
                true
            }
            ScreenState.HOME -> false
        }
    }

    // --- Recently Played Management ---
    fun loadRecentlyPlayed() {
        viewModelScope.launch {
            _recentlyPlayed.value = recentlyPlayedRepository.getRecentlyPlayed()
        }
    }

    fun clearRecentlyPlayed() {
        viewModelScope.launch {
            _recentlyPlayed.value = recentlyPlayedRepository.clearAll()
        }
    }

    fun openRecentlyPlayed() {
        _currentScreen.value = ScreenState.RECENTLY_PLAYED
    }

    // --- Favorite Artists Management & Suggested Playlists ---
    fun loadFavoriteArtists() {
        viewModelScope.launch {
            val favs = favoriteArtistsRepository.getFavorites()
            _favoriteArtists.value = favs
            if (favs.size >= 5) {
                settingsRepository.setArtistOnboardingCompleted(true)
                _hasCompletedArtistOnboarding.value = true
            }
            refreshSuggestedPlaylists()
        }
    }

    fun completeArtistOnboarding(artists: List<Artist>) {
        viewModelScope.launch {
            val updated = favoriteArtistsRepository.saveAllFavorites(artists)
            _favoriteArtists.value = updated
            settingsRepository.setArtistOnboardingCompleted(true)
            _hasCompletedArtistOnboarding.value = true
            refreshSuggestedPlaylists(force = true)
        }
    }

    fun openSettings() {
        _currentScreen.value = ScreenState.SETTINGS
        _isSettingsDialogOpen.value = false
    }

    fun closeSettings() {
        if (_currentScreen.value == ScreenState.SETTINGS ||
            _currentScreen.value == ScreenState.SETTINGS_NOW_PLAYING ||
            _currentScreen.value == ScreenState.SETTINGS_APP_THEME ||
            _currentScreen.value == ScreenState.SETTINGS_AUTO_SYNC
        ) {
            _currentScreen.value = ScreenState.HOME
        }
        _isSettingsDialogOpen.value = false
    }

    fun openNowPlayingSettings() {
        _currentScreen.value = ScreenState.SETTINGS_NOW_PLAYING
    }

    fun openAppThemeSettings() {
        _currentScreen.value = ScreenState.SETTINGS_APP_THEME
    }

    fun openAutoSyncSettings() {
        _currentScreen.value = ScreenState.SETTINGS_AUTO_SYNC
    }

    fun setPlayerStyle(style: PlayerStyle) {
        settingsRepository.setPlayerStyle(style)
        _playerStyle.value = style
    }

    fun setAppTheme(theme: AppTheme, variant: String = theme.defaultVariant) {
        val finalVariant = if (variant.isBlank()) theme.defaultVariant else variant
        settingsRepository.setAppTheme(theme)
        settingsRepository.setAppThemeVariant(finalVariant)
        _appTheme.value = theme
        _appThemeVariant.value = finalVariant
    }

    fun setAppThemeVariant(variant: String) {
        settingsRepository.setAppThemeVariant(variant)
        _appThemeVariant.value = variant
    }

    fun setPlayerTheme(theme: AppTheme?, variant: String? = theme?.defaultVariant) {
        val finalVariant = variant ?: theme?.defaultVariant
        settingsRepository.setPlayerTheme(theme)
        settingsRepository.setPlayerThemeVariant(finalVariant)
        _playerTheme.value = theme
        _playerThemeVariant.value = finalVariant
    }

    fun setPlayerThemeVariant(variant: String?) {
        settingsRepository.setPlayerThemeVariant(variant)
        _playerThemeVariant.value = variant
    }

    fun setCustomAccentColor(color: Color) {
        val argb = ((color.alpha * 255).toInt() shl 24) or
                   ((color.red * 255).toInt() shl 16) or
                   ((color.green * 255).toInt() shl 8) or
                   (color.blue * 255).toInt()
        settingsRepository.setCustomAccentColor(argb)
        _customAccentColor.value = color
    }

    fun setCustomThemeImage(uri: Uri) {
        viewModelScope.launch {
            val savedPath = settingsRepository.saveCustomThemeImage(uri)
            if (savedPath != null) {
                settingsRepository.setCustomThemeImagePath(savedPath)
                _customThemeImagePath.value = savedPath
                setAppTheme(AppTheme.CUSTOM_IMAGE)
            }
        }
    }

    fun refreshSuggestedPlaylists(force: Boolean = false) {
        viewModelScope.launch {
            val favs = _favoriteArtists.value
            if (favs.isEmpty()) return@launch

            // Return cached playlists immediately if available
            val cached = suggestedPlaylistsRepository.getCachedPlaylists()
            if (cached.isNotEmpty()) {
                _suggestedPlaylists.value = cached
            }

            if (force || suggestedPlaylistsRepository.shouldRefresh(favs)) {
                _isSuggestedPlaylistsLoading.value = true
                try {
                    val updated = suggestedPlaylistsRepository.generateOrRefreshPlaylists(favs)
                    _suggestedPlaylists.value = updated
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isSuggestedPlaylistsLoading.value = false
                }
            }
        }
    }

    fun addFavoriteArtist(artist: Artist) {
        viewModelScope.launch {
            val updated = favoriteArtistsRepository.addFavorite(artist)
            _favoriteArtists.value = updated
            refreshSuggestedPlaylists(force = true)
        }
    }

    fun removeFavoriteArtist(artistId: String) {
        viewModelScope.launch {
            val updated = favoriteArtistsRepository.removeFavorite(artistId)
            _favoriteArtists.value = updated
            refreshSuggestedPlaylists(force = true)
        }
    }

    fun toggleFavoriteArtist(artist: Artist) {
        viewModelScope.launch {
            val isFav = _favoriteArtists.value.any { it.id == artist.id }
            val updated = if (isFav) {
                favoriteArtistsRepository.removeFavorite(artist.id)
            } else {
                favoriteArtistsRepository.addFavorite(artist)
            }
            _favoriteArtists.value = updated
            refreshSuggestedPlaylists(force = true)
        }
    }

    fun isFavoriteArtist(artistId: String): Boolean {
        return _favoriteArtists.value.any { it.id == artistId }
    }

    // --- Dedicated Search & Search History ---
    fun openSearch() {
        _searchQuery.value = ""
        _searchResult.value = SearchResult()
        loadRecentSearches()
        _currentScreen.value = ScreenState.SEARCH
    }

    fun closeSearch() {
        _currentScreen.value = ScreenState.HOME
        _searchQuery.value = ""
        _searchResult.value = SearchResult()
    }

    fun loadRecentSearches() {
        viewModelScope.launch {
            _recentSearches.value = searchHistoryRepository.getRecentSearches()
        }
    }

    fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            val updated = searchHistoryRepository.addSearch(query)
            _recentSearches.value = updated
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            val updated = searchHistoryRepository.removeSearch(query)
            _recentSearches.value = updated
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            val updated = searchHistoryRepository.clearAll()
            _recentSearches.value = updated
        }
    }

    // --- Artist Customization & Search ---
    fun onArtistSearchQueryChanged(query: String) {
        _artistSearchQuery.value = query
        artistSearchJob?.cancel()
        if (query.isBlank()) {
            _artistSearchResults.value = emptyList()
            _isArtistSearching.value = false
            return
        }
        artistSearchJob = viewModelScope.launch {
            delay(250)
            _isArtistSearching.value = true
            val results = ytRepository.searchArtists(query)
            _artistSearchResults.value = results
            _isArtistSearching.value = false
        }
    }

    fun addArtist(artist: Artist) {
        viewModelScope.launch {
            val updated = artistRepository.addArtist(artist)
            _artists.value = updated
        }
    }

    fun removeArtist(artist: Artist) {
        viewModelScope.launch {
            val updated = artistRepository.removeArtist(artist.id)
            _artists.value = updated
        }
    }

    fun toggleArtistVisibility(artist: Artist) {
        viewModelScope.launch {
            val updated = artistRepository.toggleArtistVisibility(artist.id)
            _artists.value = updated
        }
    }

    fun resetArtistsToDefault() {
        viewModelScope.launch {
            val updated = artistRepository.resetToDefault()
            _artists.value = updated
        }
    }

    fun openArtistManager() {
        _currentScreen.value = ScreenState.ARTIST_MANAGER
    }

    fun closeArtistManager() {
        _currentScreen.value = ScreenState.HOME
        _artistSearchQuery.value = ""
        _artistSearchResults.value = emptyList()
    }

    // --- Local Tracks ---
    fun loadLocalTracks() {
        viewModelScope.launch {
            _isLocalLoading.value = true
            val tracks = localRepository.getLocalAudioTracks()
            _localTracks.value = tracks
            _isLocalLoading.value = false
        }
    }

    fun onFilesPicked(uris: List<Uri>) {
        viewModelScope.launch {
            _isLocalLoading.value = true
            val newTracks = mutableListOf<Track>()
            for (uri in uris) {
                val track = localRepository.getTrackFromUri(uri)
                if (track != null) {
                    newTracks.add(track)
                }
            }
            if (newTracks.isNotEmpty()) {
                val combined = (_localTracks.value + newTracks).distinctBy { it.id }
                _localTracks.value = combined
                playTrack(newTracks.first(), combined)
            }
            _isLocalLoading.value = false
        }
    }

    fun setLocalFormatFilter(filter: String) {
        _localFormatFilter.value = filter
    }

    fun getFilteredLocalTracks(): List<Track> {
        val all = _localTracks.value
        return when (_localFormatFilter.value) {
            "FLAC" -> all.filter { it.audioFormat == AudioFormat.FLAC }
            "M4A" -> all.filter { it.audioFormat == AudioFormat.M4A }
            "MP3" -> all.filter { it.audioFormat == AudioFormat.MP3 }
            else -> all
        }
    }

    // --- Playback Controls ---
    fun playTrack(track: Track, queue: List<Track>? = null) {
        if (queue != null) {
            _isQueueReorderable.value = queue.size > 1
        } else if (playerManager.queue.value.size <= 1) {
            _isQueueReorderable.value = false
        }
        recordTrackPlayed(track)
        playerManager.playTrack(track, queue)
    }

    fun playPlaylistTrack(playlist: Playlist, track: Track) {
        _isQueueReorderable.value = playlist.tracks.size > 1
        recordTrackPlayed(track)
        playerManager.playTrack(track, playlist.tracks)
    }

    fun playAllFromPlaylist(playlist: Playlist) {
        _isQueueReorderable.value = playlist.tracks.size > 1
        if (playlist.tracks.isNotEmpty()) {
            recordTrackPlayed(playlist.tracks.first())
            playerManager.playTrack(playlist.tracks.first(), playlist.tracks)
        }
    }

    fun shuffleAllFromPlaylist(playlist: Playlist) {
        _isQueueReorderable.value = playlist.tracks.size > 1
        if (playlist.tracks.isNotEmpty()) {
            val shuffled = playlist.tracks.shuffled()
            recordTrackPlayed(shuffled.first())
            playerManager.playTrack(shuffled.first(), shuffled)
        }
    }

    private fun recordTrackPlayed(track: Track) {
        viewModelScope.launch {
            val updated = recentlyPlayedRepository.addTrack(track)
            _recentlyPlayed.value = updated
            playCountRepository.recordPlay(track)
        }
    }

    fun playAll(tracks: List<Track>) {
        _isQueueReorderable.value = tracks.size > 1
        if (tracks.isNotEmpty()) {
            recordTrackPlayed(tracks.first())
            playerManager.playTrack(tracks.first(), tracks)
        }
    }

    fun shuffleAll(tracks: List<Track>) {
        _isQueueReorderable.value = tracks.size > 1
        if (tracks.isNotEmpty()) {
            val shuffled = tracks.shuffled()
            recordTrackPlayed(shuffled.first())
            playerManager.playTrack(shuffled.first(), shuffled)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun nextTrack() {
        playerManager.nextTrack()
    }

    fun previousTrack() {
        playerManager.previousTrack()
    }

    fun seekToFraction(fraction: Float) {
        val dur = playbackState.value.durationMs
        if (dur > 0) {
            val targetMs = (dur * fraction).toLong()
            playerManager.seekTo(targetMs)
        }
    }

    fun seekToPosition(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    fun openFullPlayer() {
        _isFullPlayerOpen.value = true
    }

    fun closeFullPlayer() {
        _isFullPlayerOpen.value = false
    }

    // --- Queue Management ---
    fun playNext(track: Track) {
        playerManager.playNext(track)
    }

    fun addToQueue(track: Track) {
        playerManager.addToQueue(track)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        playerManager.reorderQueue(fromIndex, toIndex)
    }

    fun removeFromQueue(index: Int) {
        playerManager.removeFromQueue(index)
    }

    // --- Playlist Management ---
    fun loadPlaylists() {
        viewModelScope.launch {
            _playlists.value = playlistRepository.getPlaylists()
        }
    }

    fun createPlaylist(title: String, imageUri: Uri?, initialTrack: Track? = null) {
        viewModelScope.launch {
            val trackToAdd = initialTrack ?: _targetTrackForPlaylist.value
            val savedImagePath = imageUri?.let { playlistRepository.copyImageToInternalStorage(it) }
            val created = playlistRepository.createPlaylist(title, savedImagePath, trackToAdd)
            val all = playlistRepository.getPlaylists()
            _playlists.value = all
            if (_selectedPlaylist.value?.id == created.id) {
                _selectedPlaylist.value = created
            }
            _isNewPlaylistSheetOpen.value = false
            _targetTrackForPlaylist.value = null

            // Mirror newly created playlist to YouTube Music if logged in
            if (!ytRepository.cookie.isNullOrBlank()) {
                pushLocalPlaylistToYouTube(created)
            }
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            val updated = playlistRepository.addTrackToPlaylist(playlistId, track)
            _playlists.value = updated
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = updated.find { it.id == playlistId }
            }
            _isExistingPlaylistSheetOpen.value = false
            _targetTrackForPlaylist.value = null

            // Mirror track addition to YouTube Music if logged in
            if (!ytRepository.cookie.isNullOrBlank()) {
                if (playlistId.startsWith("yt_sync_")) {
                    val ytPlaylistId = playlistId.removePrefix("yt_sync_")
                    val videoId = ytRepository.resolveVideoIdForTrack(track)
                    if (!videoId.isNullOrBlank()) {
                        ytRepository.addVideosToYouTubePlaylist(ytPlaylistId, listOf(videoId))
                    }
                } else {
                    val targetLocal = updated.find { it.id == playlistId }
                    if (targetLocal != null) {
                        pushLocalPlaylistToYouTube(targetLocal)
                    }
                }
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            val existingPlaylist = _playlists.value.find { it.id == playlistId }
            val removedTrack = existingPlaylist?.tracks?.find { it.id == trackId }

            val updated = playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
            _playlists.value = updated
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = updated.find { it.id == playlistId }
            }

            // Mirror track removal to YouTube Music if logged in
            if (!ytRepository.cookie.isNullOrBlank() && playlistId.startsWith("yt_sync_")) {
                val ytPlaylistId = playlistId.removePrefix("yt_sync_")
                val videoId = removedTrack?.let { ytRepository.resolveVideoIdForTrack(it) } ?: trackId
                if (videoId.isNotBlank()) {
                    ytRepository.removeVideoFromYouTubePlaylist(ytPlaylistId, videoId)
                }
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            val updated = playlistRepository.deletePlaylist(playlistId)
            _playlists.value = updated
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
                _currentScreen.value = ScreenState.HOME
            }

            // Mirror playlist deletion to YouTube Music if logged in
            if (!ytRepository.cookie.isNullOrBlank() && playlistId.startsWith("yt_sync_")) {
                val ytPlaylistId = playlistId.removePrefix("yt_sync_")
                ytRepository.deleteYouTubePlaylist(ytPlaylistId)
            }
        }
    }

    fun editPlaylist(playlistId: String, newTitle: String, newImageUri: Uri?, removeImage: Boolean) {
        viewModelScope.launch {
            val current = _playlists.value.find { it.id == playlistId } ?: return@launch
            val oldTitle = current.title
            val finalImagePath = when {
                removeImage -> null
                newImageUri != null -> playlistRepository.copyImageToInternalStorage(newImageUri)
                else -> current.imageUri
            }
            val updated = playlistRepository.updatePlaylist(playlistId, newTitle, finalImagePath)
            _playlists.value = updated
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = updated.find { it.id == playlistId }
            }

            // Mirror playlist rename / creation to YouTube Music if logged in
            if (!ytRepository.cookie.isNullOrBlank()) {
                if (playlistId.startsWith("yt_sync_")) {
                    if (oldTitle != newTitle && newTitle.isNotBlank()) {
                        val ytPlaylistId = playlistId.removePrefix("yt_sync_")
                        ytRepository.renameYouTubePlaylist(ytPlaylistId, newTitle)
                    }
                } else {
                    val updatedLocal = updated.find { it.id == playlistId }
                    if (updatedLocal != null) {
                        pushLocalPlaylistToYouTube(updatedLocal)
                    }
                }
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        _currentScreen.value = ScreenState.PLAYLIST_DETAIL
    }

    fun reorderTracksInPlaylist(playlistId: String, from: Int, to: Int) {
        viewModelScope.launch {
            val updated = playlistRepository.reorderTracksInPlaylist(playlistId, from, to)
            _playlists.value = updated
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = updated.find { it.id == playlistId }
            }
        }
    }

    fun exportPlaylistAsJson(playlist: Playlist): String {
        return playlistRepository.exportPlaylistToJson(playlist)
    }

    fun importPlaylistFromJson(jsonString: String) {
        viewModelScope.launch {
            val playlist = playlistRepository.importPlaylistFromJson(jsonString) ?: return@launch
            val all = playlistRepository.saveImportedPlaylist(playlist)
            _playlists.value = all
            if (!ytRepository.cookie.isNullOrBlank() && !playlist.id.startsWith("yt_sync_")) {
                pushLocalPlaylistToYouTube(playlist)
            }
        }
    }

    // --- YouTube Playlist Import ---
    fun fetchYouTubePlaylistPreview(urlOrId: String) {
        val cleanId = YouTubeRepository.extractPlaylistId(urlOrId) ?: urlOrId.trim()
        if (cleanId.isEmpty()) {
            _ytPlaylistImportError.value = "Please enter a valid YouTube or YouTube Music playlist link"
            return
        }

        viewModelScope.launch {
            _isImportingYtPlaylist.value = true
            _ytPlaylistImportError.value = null
            _ytPlaylistPreview.value = null

            try {
                val data = ytRepository.fetchPlaylistFromYouTube(cleanId)
                if (data != null && data.tracks.isNotEmpty()) {
                    _ytPlaylistPreview.value = data
                } else if (data != null && data.tracks.isEmpty()) {
                    _ytPlaylistImportError.value = "Playlist is empty or private"
                } else {
                    _ytPlaylistImportError.value = "Could not load playlist. Please check the URL and try again"
                }
            } catch (e: Exception) {
                _ytPlaylistImportError.value = "Failed to fetch playlist: ${e.message}"
            } finally {
                _isImportingYtPlaylist.value = false
            }
        }
    }

    fun confirmImportYouTubePlaylist(customTitle: String? = null, onComplete: ((Playlist) -> Unit)? = null) {
        val preview = _ytPlaylistPreview.value ?: return
        viewModelScope.launch {
            _isImportingYtPlaylist.value = true
            try {
                val finalTitle = customTitle?.trim()?.ifEmpty { null } ?: preview.title
                val savedImagePath = preview.thumbnailUrl?.let { playlistRepository.downloadImageToInternalStorage(it) }
                val created = playlistRepository.createPlaylistWithTracks(finalTitle, savedImagePath, preview.tracks)
                val all = playlistRepository.getPlaylists()
                _playlists.value = all
                _ytPlaylistPreview.value = null
                _ytPlaylistImportError.value = null
                _isNewPlaylistSheetOpen.value = false
                if (!ytRepository.cookie.isNullOrBlank()) {
                    pushLocalPlaylistToYouTube(created)
                }
                val latestCreated = _playlists.value.find { it.title == finalTitle } ?: created
                onComplete?.invoke(latestCreated)
            } catch (e: Exception) {
                _ytPlaylistImportError.value = "Error saving playlist: ${e.message}"
            } finally {
                _isImportingYtPlaylist.value = false
            }
        }
    }

    fun clearYouTubePlaylistPreview() {
        _ytPlaylistPreview.value = null
        _ytPlaylistImportError.value = null
        _isImportingYtPlaylist.value = false
    }

    // --- Song Context Menu & Modal States ---
    fun openSongMenu(track: Track) {
        _contextMenuTrack.value = track
    }

    fun closeSongMenu() {
        _contextMenuTrack.value = null
    }

    fun openExistingPlaylistSheet(track: Track? = null) {
        val target = track ?: _contextMenuTrack.value ?: _targetTrackForPlaylist.value
        _targetTrackForPlaylist.value = target
        _contextMenuTrack.value = null
        _isExistingPlaylistSheetOpen.value = true
    }

    fun closeExistingPlaylistSheet() {
        _isExistingPlaylistSheetOpen.value = false
        _targetTrackForPlaylist.value = null
    }

    fun openNewPlaylistSheet(track: Track? = null) {
        val target = track ?: _contextMenuTrack.value ?: _targetTrackForPlaylist.value
        _targetTrackForPlaylist.value = target
        _contextMenuTrack.value = null
        _isNewPlaylistSheetOpen.value = true
    }

    fun closeNewPlaylistSheet() {
        _isNewPlaylistSheetOpen.value = false
        _targetTrackForPlaylist.value = null
        clearYouTubePlaylistPreview()
    }

    // --- In-App Updates ---
    fun checkForUpdates(manual: Boolean = true) {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val info = updateManager.checkLatestRelease(currentAppVersion)
            _isCheckingUpdate.value = false
            if (info != null && info.isUpdateAvailable) {
                _updateInfo.value = info
            } else if (manual) {
                _updateToastMessage.value = if (info != null) {
                    "You're on the latest version (v${info.latestVersion})"
                } else {
                    "Unable to check for updates"
                }
            }
        }
    }

    fun clearUpdateToast() {
        _updateToastMessage.value = null
    }

    fun dismissUpdateDialog() {
        if (_downloadProgress.value == null) {
            _updateInfo.value = null
        }
    }

    fun startUpdateDownload(context: Context) {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            _downloadProgress.value = 0.01f
            val file = updateManager.downloadApk(context, info.apkDownloadUrl) { progress ->
                _downloadProgress.value = progress
            }
            if (file != null && file.exists()) {
                _downloadProgress.value = 1.0f
                delay(400)
                updateManager.installApk(context, file)
                _downloadProgress.value = null
                _updateInfo.value = null
            } else {
                _downloadProgress.value = null
                _updateToastMessage.value = "Failed to download update"
            }
        }
    }
}
