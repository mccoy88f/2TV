package com.twotv.tv.server

import android.content.Context
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

@Serializable
data class TvPlayPayload(
    val command: String = "PLAY",
    val mediaType: String = "VIDEO",
    val url: String = "",
    val title: String = "",
    val saveToTv: Boolean = false
)

@Serializable
data class DevicePairInfo(
    val deviceName: String = "Mobile Device",
    val deviceIp: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class TvEmbeddedServer(
    private val context: Context,
    private val port: Int = 8080,
    private val pairingToken: String,
    private val onPlayMedia: (TvPlayPayload) -> Unit,
    private val onDevicePaired: (device: DevicePairInfo, isSilent: Boolean) -> Unit,
    private val onUploadProgress: (title: String, percentage: Int) -> Unit = { _, _ -> },
    private val onUploadFinished: () -> Unit = {}
) {
    private var server: EmbeddedServer<*, *>? = null
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun start() {
        server = embeddedServer(CIO, port = port) {
            routing {
                get("/api/ping") {
                    val clientIp = call.request.origin.remoteHost
                    val deviceName = call.request.headers["X-Device-Name"] ?: "Smartphone"
                    onDevicePaired(DevicePairInfo(deviceName = deviceName, deviceIp = clientIp), true)
                    call.respondText(
                        text = """{"success":true,"message":"online"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                }

                post("/api/pair") {
                    val clientIp = call.request.origin.remoteHost
                    try {
                        val bodyText = call.receiveText()
                        val body = json.decodeFromString<DevicePairInfo>(bodyText)
                        val pairInfo = DevicePairInfo(deviceName = body.deviceName, deviceIp = clientIp)
                        onDevicePaired(pairInfo, false)
                    } catch (e: Exception) {
                        val pairInfo = DevicePairInfo(deviceName = "Mobile Device", deviceIp = clientIp)
                        onDevicePaired(pairInfo, false)
                    }
                    call.respondText(
                        text = """{"success":true,"message":"Device paired"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                }

                post("/api/verify") {
                    val clientIp = call.request.origin.remoteHost
                    onDevicePaired(DevicePairInfo(deviceName = "Mobile Device", deviceIp = clientIp), false)
                    call.respondText(
                        text = """{"success":true,"message":"Token valid"}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                }

                post("/api/play") {
                    val clientIp = call.request.origin.remoteHost
                    onDevicePaired(DevicePairInfo(deviceName = "Mobile Device", deviceIp = clientIp), true)

                    try {
                        val bodyText = call.receiveText()
                        val payload = json.decodeFromString<TvPlayPayload>(bodyText)
                        onPlayMedia(payload)
                        call.respondText(
                            text = """{"success":true,"message":"Playback started"}""",
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK
                        )
                    } catch (e: Exception) {
                        call.respondText(
                            text = """{"success":false,"message":"Error: ${e.localizedMessage}"}""",
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }

                post("/api/upload") {
                    val tokenHeader = call.request.headers["X-Pairing-Token"]
                    if (tokenHeader != pairingToken) {
                        call.respondText(
                            text = """{"success":false,"message":"Unauthorized"}""",
                            status = HttpStatusCode.Unauthorized
                        )
                        return@post
                    }

                    try {
                        val multipart = call.receiveMultipart()
                        var title = "File Ricevuto"
                        var mediaType = "FILE"
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
                                    val buffer = ByteArray(32768)

                                    val inputStream: InputStream = part.provider().toInputStream()

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
                            call.respondText(
                                text = """{"success":true,"message":"File uploaded and playing"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.OK
                            )
                        } else {
                            call.respondText(
                                text = """{"success":false,"message":"No file uploaded"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.BadRequest
                            )
                        }
                    } catch (e: Exception) {
                        onUploadFinished()
                        call.respondText(
                            text = """{"success":false,"message":"Upload failed: ${e.localizedMessage}"}""",
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}
