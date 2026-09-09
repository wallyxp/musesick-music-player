package com.wally.musesick.repository

import android.content.Context
import com.wally.musesick.model.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class FavoriteArtistsRepository(private val context: Context) {

    private val favoritesFile: File
        get() = File(context.filesDir, "favorite_artists.json")

    suspend fun getFavorites(): List<Artist> = withContext(Dispatchers.IO) {
        if (!favoritesFile.exists()) return@withContext emptyList()
        try {
            val jsonString = favoritesFile.readText()
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
            emptyList()
        }
    }

    private suspend fun saveFavorites(artists: List<Artist>) = withContext(Dispatchers.IO) {
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
            favoritesFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addFavorite(artist: Artist): List<Artist> = withContext(Dispatchers.IO) {
        val current = getFavorites().toMutableList()
        val index = current.indexOfFirst { it.id == artist.id || it.name.equals(artist.name, ignoreCase = true) }
        if (index >= 0) {
            current[index] = current[index].copy(
                thumbnailUrl = artist.thumbnailUrl ?: current[index].thumbnailUrl,
                browseId = artist.browseId ?: current[index].browseId
            )
        } else {
            current.add(artist)
        }
        saveFavorites(current)
        current
    }

    suspend fun removeFavorite(artistId: String): List<Artist> = withContext(Dispatchers.IO) {
        val current = getFavorites().filter { it.id != artistId }
        saveFavorites(current)
        current
    }

    suspend fun isFavorite(artistId: String): Boolean = withContext(Dispatchers.IO) {
        getFavorites().any { it.id == artistId }
    }
}

