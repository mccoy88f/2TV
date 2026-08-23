package com.twotv.app.network

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.twotv.app.data.model.MediaPayload
import com.twotv.app.data.model.MediaType
import com.twotv.app.data.model.PairedTv
import com.twotv.app.data.model.TvResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

@Serializable
data class DevicePairRequest(
    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val timestamp: Long = System.currentTimeMillis()
)

class TvSenderClient {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 300000 // 5 minutes timeout for large file uploads
        }
    }

    suspend fun sendPairingRequest(tv: PairedTv): Result<TvResponse> {
        return try {
            val urlString = "http://${tv.ip}:${tv.port}/api/pair"
            val httpResponse = httpClient.post(urlString) {
                contentType(ContentType.Application.Json)
                header("X-Pairing-Token", tv.pairingToken)
                setBody(DevicePairRequest())
            }
            if (httpResponse.status == HttpStatusCode.OK) {
                Result.success(httpResponse.body())
            } else {
                Result.failure(Exception("Status ${httpResponse.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendContentToTv(tv: PairedTv, payload: MediaPayload): Result<TvResponse> {
        return try {
            val urlString = "http://${tv.ip}:${tv.port}/api/play"
            val httpResponse = httpClient.post(urlString) {
                contentType(ContentType.Application.Json)
                header("X-Pairing-Token", tv.pairingToken)
                setBody(payload)
            }

            if (httpResponse.status == HttpStatusCode.OK) {
                val response: TvResponse = httpResponse.body()
                if (response.success) {
                    Result.success(response)
                } else {
                    Result.failure(Exception(response.message))
                }
            } else {
                val errText = try { httpResponse.bodyAsText() } catch (e: Exception) { "Status ${httpResponse.status}" }
                Result.failure(Exception("Errore TV (${httpResponse.status.value}): $errText"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Impossibile connettersi alla TV ${tv.name} (${tv.ip}:${tv.port}): ${e.localizedMessage}"))
        }
    }

    suspend fun uploadFileToTv(
        context: Context,
        tv: PairedTv,
        fileUri: Uri,
        title: String,
        mediaType: MediaType,
        saveToTv: Boolean
    ): Result<TvResponse> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure(Exception("Impossibile aprire il file selezionato"))

            val fileName = title.ifBlank { getFileNameFromUri(context, fileUri) }
            val tempCacheFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
            tempCacheFile.parentFile?.mkdirs()

            // Stream file to temp cache file first to prevent OOM
            inputStream.use { input ->
                tempCacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val fileBytes = tempCacheFile.readBytes()
            tempCacheFile.delete() // Clean cache

            val urlString = "http://${tv.ip}:${tv.port}/api/upload"
            val httpResponse = httpClient.submitFormWithBinaryData(
                url = urlString,
                formData = formData {
                    append("mediaType", mediaType.name)
                    append("title", fileName)
                    append("saveToTv", saveToTv.toString())
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        append(HttpHeaders.ContentType, getMimeType(mediaType))
                    })
                }
            ) {
                header("X-Pairing-Token", tv.pairingToken)
            }

            if (httpResponse.status == HttpStatusCode.OK) {
                val response: TvResponse = httpResponse.body()
                if (response.success) {
                    Result.success(response)
                } else {
                    Result.failure(Exception(response.message))
                }
            } else {
                val errText = try { httpResponse.bodyAsText() } catch (e: Exception) { "Status ${httpResponse.status}" }
                Result.failure(Exception("Errore risposta TV (${httpResponse.status.value}): $errText"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Errore durante l'invio del file alla TV ${tv.name}: ${e.localizedMessage}"))
        }
    }

    suspend fun pingTv(ip: String, port: Int): Boolean {
        return try {
            val urlString = "http://$ip:$port/api/ping"
            val response = httpClient.get(urlString)
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName = ""
        try {
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex) ?: ""
                        }
                    }
                }
            }
            if (fileName.isBlank()) {
                fileName = uri.lastPathSegment ?: ""
            }
        } catch (e: Exception) {
            fileName = "file_${System.currentTimeMillis()}"
        }
        return fileName
    }

    private fun getMimeType(type: MediaType): String = when (type) {
        MediaType.VIDEO -> "video/mp4"
        MediaType.IMAGE -> "image/jpeg"
        MediaType.AUDIO -> "audio/mpeg"
        MediaType.STREAM -> "application/x-mpegURL"
    }
}
