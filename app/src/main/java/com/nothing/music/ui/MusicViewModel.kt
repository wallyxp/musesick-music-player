package com.nothing.music.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nothing.music.model.AudioFormat
import com.nothing.music.model.PlaybackState
import com.nothing.music.model.Track
import com.nothing.music.player.PlayerManager
import com.nothing.music.repository.LocalAudioRepository
import com.nothing.music.repository.YouTubeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val ytRepository = YouTubeRepository()
    private val localRepository = LocalAudioRepository(application)
    val playerManager = PlayerManager.getInstance(application)

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val queue: StateFlow<List<Track>> = playerManager.queue

    private val _selectedTab = MutableStateFlow(0) // 0 = STREAM, 1 = LOCAL
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // YouTube Stream State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _ytTracks = MutableStateFlow<List<Track>>(emptyList())
    val ytTracks: StateFlow<List<Track>> = _ytTracks.asStateFlow()

    private val _isYtLoading = MutableStateFlow(false)
    val isYtLoading: StateFlow<Boolean> = _isYtLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("TOP HITS")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

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
        // Automatically fetch initial curated category so user can straight away play music!
        loadCategory("TOP HITS")
        loadLocalTracks()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
        if (index == 1 && _localTracks.value.isEmpty()) {
            loadLocalTracks()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            loadCategory(_selectedCategory.value)
            return
        }
        searchJob = viewModelScope.launch {
            _isYtLoading.value = true
            val results = ytRepository.searchTracks(query)
            _ytTracks.value = results
            _isYtLoading.value = false
        }
    }

    fun loadCategory(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = ""
        viewModelScope.launch {
            _isYtLoading.value = true
            val results = ytRepository.searchTracks("$category music")
            _ytTracks.value = results
            _isYtLoading.value = false
        }
    }

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
                // Start playing first picked track immediately!
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

    fun playTrack(track: Track, queue: List<Track>? = null) {
        playerManager.playTrack(track, queue)
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
}

