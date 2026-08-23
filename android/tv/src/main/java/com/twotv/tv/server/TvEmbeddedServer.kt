package com.twotv.tv.server

import android.content.Context
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class TvPlayPayload(
    val command: String = "PLAY",
    val mediaType: String,
    val url: String,
    val title: String,
    val saveToTv: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TvSimpleResponse(
    val success: Boolean,
    val message: String
)

class TvEmbeddedServer(
    private val context: Context,
    val port: Int = 8080,
    val pairingToken: String = "2tv-secret-tv-token",
    private val onPlayMedia: (TvPlayPayload) -> Unit
) {
    private var server: EmbeddedServer<*, *>? = null

    fun start() {
        if (server != null) return

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }

            routing {
                get("/api/ping") {
                    call.respond(TvSimpleResponse(true, "Android TV 2TV Receiver Online"))
                }

                get("/api/verify") {
                    val token = call.request.headers["X-Pairing-Token"]
                    if (token == pairingToken || token == null) {
                        call.respond(HttpStatusCode.OK, TvSimpleResponse(true, "Verified"))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, TvSimpleResponse(false, "Invalid token"))
                    }
                }

                post("/api/play") {
                    try {
                        val payload = call.receive<TvPlayPayload>()
                        onPlayMedia(payload)
                        call.respond(TvSimpleResponse(true, "Riproduzione avviata per: ${payload.title}"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, TvSimpleResponse(false, "Payload non valido: ${e.localizedMessage}"))
                    }
                }

                post("/api/upload") {
                    try {
                        val multipart = call.receiveMultipart()
                        var mediaType = "VIDEO"
                        var title = "File Ricevuto"
                        var saveToTv = false
                        var savedFile: File? = null

                        val mediaDir = File(context.filesDir, "received_media").apply { mkdirs() }

                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> {
                                    when (part.name) {
                                        "mediaType" -> mediaType = part.value
                                        "title" -> title = part.value
                                        "saveToTv" -> saveToTv = part.value.toBoolean()
                                    }
                                }
                                is PartData.FileItem -> {
                                    val fileName = part.originalFileName ?: "file_${System.currentTimeMillis()}"
                                    val destFile = File(mediaDir, fileName)
                                    withContext(Dispatchers.IO) {
                                        part.streamProvider().use { input ->
                                            destFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                    }
                                    savedFile = destFile
                                }
                                else -> {}
                            }
                            part.dispose()
                        }

                        if (savedFile != null) {
                            val payload = TvPlayPayload(
                                command = "PLAY",
                                mediaType = mediaType,
                                url = savedFile!!.absolutePath,
                                title = title.ifBlank { savedFile!!.name },
                                saveToTv = saveToTv
                            )
                            onPlayMedia(payload)
                            call.respond(TvSimpleResponse(true, "File caricato e avviato: ${savedFile!!.name}"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, TvSimpleResponse(false, "Nessun file ricevuto"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, TvSimpleResponse(false, "Errore upload: ${e.localizedMessage}"))
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
