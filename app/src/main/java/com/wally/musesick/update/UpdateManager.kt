package com.wally.musesick.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSize: Long,
    val isUpdateAvailable: Boolean
) {
    val formattedSize: String
        get() {
            if (apkSize <= 0) return ""
            val mb = apkSize.toDouble() / (1024 * 1024)
            return String.format("%.1f MB", mb)
        }
}

class UpdateManager(
    private val repoOwner: String = "wallyxp",
    private val repoName: String = "musesick-music-player"
) {

    suspend fun checkLatestRelease(currentVersion: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Musesick-Updater")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (conn.responseCode != 200) {
                return@withContext null
            }

            val jsonString = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val root = JSONObject(jsonString)

            val tagName = root.optString("tag_name", "").trim()
            val releaseName = root.optString("name", "New Release").ifEmpty { "Release $tagName" }
            val body = root.optString("body", "No release notes provided.")

            // Find APK in release assets
            val assets = root.optJSONArray("assets")
            var apkUrl: String? = null
            var apkSize: Long = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (tagName.isEmpty() || apkUrl.isNullOrEmpty()) {
                return@withContext null
            }

            val hasUpdate = isNewerVersion(currentVersion, tagName)

            AppUpdateInfo(
                latestVersion = tagName,
                releaseTitle = releaseName,
                releaseNotes = body,
                apkDownloadUrl = apkUrl,
                apkSize = apkSize,
                isUpdateAvailable = hasUpdate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Musesick-Updater")
                connectTimeout = 15000
                readTimeout = 30000
            }

            // Follow redirect if 301/302/307 (GitHub asset downloads redirect to AWS S3)
            var actualConn = conn
            var responseCode = actualConn.responseCode
            var redirects = 0
            while ((responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308) && redirects < 5
            ) {
                val newUrl = actualConn.getHeaderField("Location")
                actualConn.disconnect()
                actualConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Musesick-Updater")
                    connectTimeout = 15000
                    readTimeout = 30000
                }
                responseCode = actualConn.responseCode
                redirects++
            }

            if (responseCode != 200) {
                return@withContext null
            }

            val totalBytes = actualConn.contentLength.toLong()
            val outputFile = File(context.cacheDir, "Musesick_update.apk")
            if (outputFile.exists()) outputFile.delete()

            actualConn.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = totalRead.toFloat() / totalBytes
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            // Android 8.0+ unknown sources permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun isNewerVersion(current: String, remote: String): Boolean {
            val curClean = current.removePrefix("v").trim()
            val remClean = remote.removePrefix("v").trim()

            val curParts = curClean.split(".").mapNotNull { it.toIntOrNull() }
            val remParts = remClean.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(curParts.size, remParts.size)
            for (i in 0 until maxLen) {
                val c = curParts.getOrElse(i) { 0 }
                val r = remParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
