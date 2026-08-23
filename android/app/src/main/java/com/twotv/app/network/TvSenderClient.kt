package com.twotv.app.network

import android.content.Context
import android.net.Uri
import android.os.Build
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
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
            requestTimeout = 120000
        }
    }

    suspend fun sendPairingRequest(tv: PairedTv): Result<TvResponse> {
        return try {
            val urlString = "http://${tv.ip}:${tv.port}/api/pair"
            val response: TvResponse = httpClient.post(urlString) {
                contentType(ContentType.Application.Json)
                header("X-Pairing-Token", tv.pairingToken)
                setBody(DevicePairRequest())
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendContentToTv(tv: PairedTv, payload: MediaPayload): Result<TvResponse> {
        return try {
            val urlString = "http://${tv.ip}:${tv.port}/api/play"
            val response: TvResponse = httpClient.post(urlString) {
                contentType(ContentType.Application.Json)
                header("X-Pairing-Token", tv.pairingToken)
                setBody(payload)
            }.body()

            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
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

            val bytes = inputStream.use { it.readBytes() }
            val fileName = title.ifBlank { "media_file_${System.currentTimeMillis()}" }

            val urlString = "http://${tv.ip}:${tv.port}/api/upload"
            val response: TvResponse = httpClient.submitFormWithBinaryData(
                url = urlString,
                formData = formData {
                    append("mediaType", mediaType.name)
                    append("title", title)
                    append("saveToTv", saveToTv.toString())
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        append(HttpHeaders.ContentType, getMimeType(mediaType))
                    })
                }
            ) {
                header("X-Pairing-Token", tv.pairingToken)
            }.body()

            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
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

    private fun getMimeType(type: MediaType): String = when (type) {
        MediaType.VIDEO -> "video/mp4"
        MediaType.IMAGE -> "image/jpeg"
        MediaType.AUDIO -> "audio/mpeg"
        MediaType.STREAM -> "application/x-mpegURL"
    }
}
