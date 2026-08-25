package com.twotv.tv.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("size") val size: Long = 0
)

data class UpdateInfo(
    val latestVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String
)

object AppUpdater {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun checkForUpdate(currentVersionName: String, isTv: Boolean): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get("https://api.github.com/repos/mccoy88f/2TV/releases/latest") {
                header("Accept", "application/vnd.github+json")
                header("User-Agent", "2TV-Android-App")
            }

            if (response.status != HttpStatusCode.OK) return@withContext null

            val bodyText = response.bodyAsText()
            val release = json.decodeFromString<GitHubRelease>(bodyText)

            val latestTag = release.tagName.removePrefix("v").trim()
            if (isNewerVersion(currentVersionName, latestTag)) {
                val assetName = if (isTv) "2TV-AndroidTV.apk" else "2TV-Mobile.apk"
                val matchingAsset = release.assets.find { it.name.equals(assetName, ignoreCase = true) }
                    ?: release.assets.firstOrNull { it.name.endsWith(".apk") }

                if (matchingAsset != null) {
                    return@withContext UpdateInfo(
                        latestVersionName = latestTag,
                        releaseNotes = release.body.ifBlank { "Nuova versione disponibile su GitHub!" },
                        downloadUrl = matchingAsset.downloadUrl
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (c > l) return false
            }
            false
        } catch (e: Exception) {
            latest != current
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        authority: String,
        onProgress: (percent: Int) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(downloadUrl) {
                header("User-Agent", "2TV-Android-App")
            }
            if (response.status != HttpStatusCode.OK) {
                return@withContext Result.failure(Exception("Errore download HTTP ${response.status}"))
            }

            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 1L
            val updateFile = File(context.cacheDir, "2tv_update.apk")
            if (updateFile.exists()) updateFile.delete()

            val channel = response.bodyAsChannel()
            var bytesCopied = 0L
            val buffer = ByteArray(32768)

            updateFile.outputStream().use { output ->
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    bytesCopied += read
                    val percent = if (contentLength > 0) ((bytesCopied * 100) / contentLength).toInt().coerceIn(0, 99) else 50
                    withContext(Dispatchers.Main) {
                        onProgress(percent)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
                val apkUri: Uri = FileProvider.getUriForFile(context, authority, updateFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
