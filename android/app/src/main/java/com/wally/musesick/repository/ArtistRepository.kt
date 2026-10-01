package com.wally.musesick.repository

import android.content.Context
import com.wally.musesick.model.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ArtistRepository(private val context: Context) {

    private val artistsFile: File
        get() = File(context.filesDir, "saved_artists.json")

    suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        if (!artistsFile.exists()) {
            val defaults = YouTubeRepository.DEFAULT_INITIAL_ARTISTS
            saveArtists(defaults)
            return@withContext defaults
        }
        try {
            val jsonString = artistsFile.readText()
            val array = JSONArray(jsonString)
            val result = mutableListOf<Artist>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    Artist(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        thumbnailUrl = obj.optString("thumbnailUrl").ifEmpty { null },
                        subtitle = obj.optString("subtitle", "Artist"),
                        isCustom = obj.optBoolean("isCustom", false),
                        isVisible = obj.optBoolean("isVisible", true),
                        browseId = obj.optString("browseId").ifEmpty { null }
                    )
                )
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            YouTubeRepository.DEFAULT_INITIAL_ARTISTS
        }
    }

    suspend fun saveArtists(artists: List<Artist>) = withContext(Dispatchers.IO) {
        try {
            val array = JSONArray()
            for (a in artists) {
                val obj = JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("thumbnailUrl", a.thumbnailUrl ?: "")
                    put("subtitle", a.subtitle)
                    put("isCustom", a.isCustom)
                    put("isVisible", a.isVisible)
                    put("browseId", a.browseId ?: "")
                }
                array.put(obj)
            }
            artistsFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addArtist(artist: Artist): List<Artist> {
        val current = getArtists().toMutableList()
        val existingIndex = current.indexOfFirst {
            it.name.equals(artist.name, ignoreCase = true) || it.id == artist.id
        }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(
                isVisible = true,
                thumbnailUrl = artist.thumbnailUrl ?: existing.thumbnailUrl,
                browseId = artist.browseId ?: existing.browseId
            )
        } else {
            current.add(0, artist.copy(isVisible = true))
        }
        saveArtists(current)
        return current
    }

    suspend fun removeArtist(artistId: String): List<Artist> {
        val current = getArtists().filter { it.id != artistId }
        saveArtists(current)
        return current
    }

    suspend fun toggleArtistVisibility(artistId: String): List<Artist> {
        val current = getArtists().map {
            if (it.id == artistId) it.copy(isVisible = !it.isVisible) else it
        }
        saveArtists(current)
        return current
    }

    suspend fun resetToDefault(): List<Artist> {
        val defaults = YouTubeRepository.DEFAULT_INITIAL_ARTISTS
        saveArtists(defaults)
        return defaults
    }
}

