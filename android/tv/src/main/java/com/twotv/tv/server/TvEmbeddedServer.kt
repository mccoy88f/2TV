package com.twotv.tv.server

import android.content.Context
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File
import java.io.InputStream

@Serializable
data class TvPlayPayload(
    val command: String,
    val mediaType: String,
    val url: String,
    val title: String,
    val saveToTv: Boolean = false
)

@Serializable
data class DevicePairInfo(
    val deviceName: String,
    val deviceIp: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TvEmbeddedServer(
    private val context: Context,
    private val port: Int = 8080,
    private val pairingToken: String,
    private val onPlayMedia: (TvPlayPayload) -> Unit,
    private val onDevicePaired: (DevicePairInfo) -> Unit,
    private val onUploadProgress: (title: String, percentage: Int) -> Unit = { _, _ -> },
    private val onUploadFinished: () -> Unit = {}
) {
    private var server: EmbeddedServer<*, *>? = null

    fun start() {
        server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json()
            }

            routing {
                get("/api/ping") {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "online", "server" to "2TV Receiver"))
                }

                post("/api/pair") {
                    val clientIp = call.request.origin.remoteHost
                    try {
                        val body = call.receive<DevicePairInfo>()
                        val pairInfo = DevicePairInfo(deviceName = body.deviceName, deviceIp = clientIp)
                        onDevicePaired(pairInfo)
                        call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Device paired successfully"))
                    } catch (e: Exception) {
                        val pairInfo = DevicePairInfo(deviceName = "Mobile Device", deviceIp = clientIp)
                        onDevicePaired(pairInfo)
                        call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Device paired"))
                    }
                }

                post("/api/verify") {
                    val token = call.request.headers["X-Pairing-Token"]
                    if (token == pairingToken || token != null) {
                        val clientIp = call.request.origin.remoteHost
                        onDevicePaired(DevicePairInfo(deviceName = "Mobile Device", deviceIp = clientIp))
                        call.respond(HttpStatusCode.OK, mapOf("valid" to true))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("valid" to false))
                    }
                }

                post("/api/play") {
                    val clientIp = call.request.origin.remoteHost
                    onDevicePaired(DevicePairInfo(deviceName = "Mobile Device", deviceIp = clientIp))

                    try {
                        val payload = call.receive<TvPlayPayload>()
                        onPlayMedia(payload)
                        call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Playback started"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to (e.localizedMessage ?: "Errore play")))
                    }
                }

                post("/api/upload") {
                    val clientIp = call.request.origin.remoteHost
                    onDevicePaired(DevicePairInfo(deviceName = "Mobile Device", deviceIp = clientIp))

                    try {
                        val multipart = call.receiveMultipart()
                        var title = "File Ricevuto"
                        var mediaType = "VIDEO"
                        var saveToTv = false
                        var savedFile: File? = null

                        val contentLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 1L

                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> {
                                    when (part.name) {
                                        "title" -> title = part.value
                                        "mediaType" -> mediaType = part.value
                                        "saveToTv" -> saveToTv = part.value.toBoolean()
                                    }
                                }
                                is PartData.FileItem -> {
                                    val originalName = part.originalFileName ?: "file_${System.currentTimeMillis()}"
                                    val fileName = if (title.isNotBlank() && title != "File Ricevuto") {
                                        if (title.contains(".")) title else "$title.${originalName.substringAfterLast(".", "bin")}"
                                    } else {
                                        originalName
                                    }

                                    val storageDir = File(context.filesDir, "media").apply { mkdirs() }
                                    val destFile = File(storageDir, fileName)

                                    var totalBytesRead = 0L
                                    val buffer = ByteArray(16384)

                                    @Suppress("DEPRECATION")
                                    val inputStream: InputStream = part.streamProvider()

                                    destFile.outputStream().use { output ->
                                        inputStream.use { input ->
                                            var bytes: Int
                                            while (input.read(buffer).also { bytes = it } != -1) {
                                                output.write(buffer, 0, bytes)
                                                totalBytesRead += bytes
                                                val percent = if (contentLength > 0) ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 99) else 50
                                                onUploadProgress(fileName, percent)
                                            }
                                        }
                                    }
                                    savedFile = destFile
                                    onUploadProgress(fileName, 100)
                                }
                                else -> {}
                            }
                            part.dispose()
                        }

                        onUploadFinished()

                        if (savedFile != null) {
                            val payload = TvPlayPayload(
                                command = "PLAY",
                                mediaType = mediaType,
                                url = savedFile!!.absolutePath,
                                title = title,
                                saveToTv = saveToTv
                            )
                            onPlayMedia(payload)
                            call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "File uploaded and playing"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "No file received"))
                        }
                    } catch (e: Exception) {
                        onUploadFinished()
                        call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to (e.localizedMessage ?: "Errore server durante l'upload")))
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop()
        server = null
    }
}
