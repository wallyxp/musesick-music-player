package com.wally.musesick.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class SearchHistoryRepository(private val context: Context) {

    private val searchHistoryFile: File
        get() = File(context.filesDir, "recent_searches.json")

    private val maxHistorySize = 20

    suspend fun getRecentSearches(): List<String> = withContext(Dispatchers.IO) {
        if (!searchHistoryFile.exists()) return@withContext emptyList()
        try {
            val jsonString = searchHistoryFile.readText()
            val array = JSONArray(jsonString)
            val result = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val query = array.getString(i)
                if (query.isNotBlank()) {
                    result.add(query)
                }
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun saveSearches(searches: List<String>) = withContext(Dispatchers.IO) {
        try {
            val array = JSONArray()
            for (q in searches) {
                array.put(q)
            }
            searchHistoryFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addSearch(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext getRecentSearches()

        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val result = if (current.size > maxHistorySize) current.take(maxHistorySize) else current
        saveSearches(result)
        result
    }

    suspend fun removeSearch(query: String): List<String> = withContext(Dispatchers.IO) {
        val current = getRecentSearches().filterNot { it.equals(query.trim(), ignoreCase = true) }
        saveSearches(current)
        current
    }

    suspend fun clearAll(): List<String> = withContext(Dispatchers.IO) {
        try {
            if (searchHistoryFile.exists()) {
                searchHistoryFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }
}

