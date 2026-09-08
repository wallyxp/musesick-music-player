package com.wally.musesick.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wally.musesick.model.Album
import com.wally.musesick.model.Artist
import com.wally.musesick.model.AudioFormat
import com.wally.musesick.model.PlaybackState
import com.wally.musesick.model.SearchResult
import com.wally.musesick.model.Track
import com.wally.musesick.player.PlayerManager
import com.wally.musesick.repository.ArtistRepository
import com.wally.musesick.repository.LocalAudioRepository
import com.wally.musesick.repository.YouTubeRepository
import com.wally.musesick.model.Playlist
import com.wally.musesick.repository.PlaylistRepository
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
    ARTIST_MANAGER
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val ytRepository = YouTubeRepository()
    private val localRepository = LocalAudioRepository(application)
    private val playlistRepository = PlaylistRepository(application)
    private val artistRepository = ArtistRepository(application)
    val playerManager = PlayerManager.getInstance(application)

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val queue: StateFlow<List<Track>> = playerManager.queue

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

    private var searchJob: Job? = null

    init {
        loadArtists()
        loadLocalTracks()
        loadPlaylists()
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
        _currentScreen.value = ScreenState.ARTIST_DETAIL

        viewModelScope.launch {
            _isArtistLoading.value = true
            val (albums, songs) = ytRepository.getArtistDetails(artist)
            _artistAlbums.value = albums
            _artistSongs.value = songs
            _isArtistLoading.value = false
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
            ScreenState.HOME -> false
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
        playerManager.playTrack(track, queue)
    }

    fun playAll(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            playerManager.playTrack(tracks.first(), tracks)
        }
    }

    fun shuffleAll(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            val shuffled = tracks.shuffled()
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
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            val updated = playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
            _playlists.value = updated
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = updated.find { it.id == playlistId }
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
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        _currentScreen.value = ScreenState.PLAYLIST_DETAIL
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
    }
}
