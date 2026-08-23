package com.twotv.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twotv.app.TwoTvApplication
import com.twotv.app.data.model.*
import com.twotv.app.network.TvSenderClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

enum class SendCategoryMode {
    FILE,
    STREAM,
    LINK
}

sealed interface SendUiState {
    object Idle : SendUiState
    object Sending : SendUiState
    data class Success(val message: String) : SendUiState
    data class Error(val errorMessage: String) : SendUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as TwoTvApplication).database
    private val tvDao = db.pairedTvDao()
    private val historyDao = db.sendHistoryDao()
    private val senderClient = TvSenderClient()

    val pairedTvs: StateFlow<List<PairedTv>> = tvDao.getAllTvs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTv: StateFlow<PairedTv?> = tvDao.getSelectedTvFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sendHistory: StateFlow<List<SendHistory>> = historyDao.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var sendCategoryMode = MutableStateFlow(SendCategoryMode.STREAM)
    var inputUrl = MutableStateFlow("")
    var inputTitle = MutableStateFlow("")
    var selectedMediaType = MutableStateFlow(MediaType.VIDEO)
    var saveToTv = MutableStateFlow(false)
    var selectedFileUri = MutableStateFlow<Uri?>(null)

    val sendUiState = MutableStateFlow<SendUiState>(SendUiState.Idle)
    val isDarkThemeOverride = MutableStateFlow<Boolean?>(null)

    fun setSendCategoryMode(mode: SendCategoryMode) {
        sendCategoryMode.value = mode
        when (mode) {
            SendCategoryMode.STREAM -> selectedMediaType.value = MediaType.VIDEO
            SendCategoryMode.LINK -> selectedMediaType.value = MediaType.STREAM
            SendCategoryMode.FILE -> {}
        }
    }

    fun setUrl(url: String) {
        inputUrl.value = url
        selectedFileUri.value = null
        val autoTitle = deriveTitleFromUrl(url)
        if (autoTitle.isNotBlank()) {
            inputTitle.value = autoTitle
        }
    }

    fun setFileUri(uri: Uri, mimeType: String?) {
        selectedFileUri.value = uri
        inputUrl.value = uri.toString()
        selectedMediaType.value = detectMediaTypeFromMime(mimeType)
        sendCategoryMode.value = SendCategoryMode.FILE
        
        val realFileName = getFileNameFromUri(getApplication(), uri)
        inputTitle.value = realFileName.ifBlank { "Media File" }
    }

    fun setTitle(title: String) {
        inputTitle.value = title
    }

    fun setMediaType(type: MediaType) {
        selectedMediaType.value = type
    }

    fun setSaveToTv(save: Boolean) {
        saveToTv.value = save
    }

    fun resetSendStatus() {
        sendUiState.value = SendUiState.Idle
    }

    fun sendCurrentContent(targetTv: PairedTv? = null) {
        val tv = targetTv ?: selectedTv.value
        if (tv == null) {
            sendUiState.value = SendUiState.Error("Nessuna TV selezionata. Abbina una TV prima di inviare!")
            return
        }

        viewModelScope.launch {
            sendUiState.value = SendUiState.Sending
            val fileUri = selectedFileUri.value
            val title = if (inputTitle.value.isNotBlank()) {
                inputTitle.value
            } else if (fileUri != null) {
                getFileNameFromUri(getApplication(), fileUri)
            } else {
                deriveTitleFromUrl(inputUrl.value)
            }

            if (sendCategoryMode.value == SendCategoryMode.FILE && fileUri != null) {
                // FILE Mode: Local File Upload Transfer to TV
                val result = senderClient.uploadFileToTv(
                    context = getApplication(),
                    tv = tv,
                    fileUri = fileUri,
                    title = title,
                    mediaType = selectedMediaType.value,
                    saveToTv = saveToTv.value
                )

                if (result.isSuccess) {
                    historyDao.insertHistory(
                        SendHistory(
                            title = title,
                            url = "[File Caricato] $title",
                            mediaType = selectedMediaType.value,
                            saveToTv = saveToTv.value,
                            targetTvName = tv.name,
                            isSuccess = true
                        )
                    )
                    sendUiState.value = SendUiState.Success("File caricato e avviato su ${tv.name}!")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Errore caricamento file"
                    sendUiState.value = SendUiState.Error(errorMsg)
                }
            } else {
                // STREAM or LINK Mode
                val url = inputUrl.value.trim()
                if (url.isEmpty()) {
                    sendUiState.value = SendUiState.Error("Inserisci un URL o seleziona un file valido")
                    return@launch
                }

                val mediaTypeString = when (sendCategoryMode.value) {
                    SendCategoryMode.STREAM -> selectedMediaType.value.name
                    SendCategoryMode.LINK -> "WEB"
                    SendCategoryMode.FILE -> selectedMediaType.value.name
                }

                val payload = MediaPayload(
                    command = "PLAY",
                    mediaType = mediaTypeString,
                    url = url,
                    title = title,
                    saveToTv = saveToTv.value
                )

                val result = senderClient.sendContentToTv(tv, payload)
                if (result.isSuccess) {
                    historyDao.insertHistory(
                        SendHistory(
                            title = title,
                            url = url,
                            mediaType = selectedMediaType.value,
                            saveToTv = saveToTv.value,
                            targetTvName = tv.name,
                            isSuccess = true
                        )
                    )
                    sendUiState.value = SendUiState.Success("Inviato con successo a ${tv.name}!")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Errore di connessione"
                    historyDao.insertHistory(
                        SendHistory(
                            title = title,
                            url = url,
                            mediaType = selectedMediaType.value,
                            saveToTv = saveToTv.value,
                            targetTvName = tv.name,
                            isSuccess = false
                        )
                    )
                    sendUiState.value = SendUiState.Error(errorMsg)
                }
            }
        }
    }

    fun resendItem(item: SendHistory) {
        inputUrl.value = item.url
        inputTitle.value = item.title
        selectedMediaType.value = item.mediaType
        saveToTv.value = item.saveToTv
        selectedFileUri.value = null
        sendCurrentContent()
    }

    fun addPairingFromQrJson(jsonString: String): Boolean {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val qrData = json.decodeFromString<PairingQrPayload>(jsonString)
            val tvId = UUID.nameUUIDFromBytes("${qrData.ip}:${qrData.port}".toByteArray()).toString()

            val pairedTv = PairedTv(
                id = tvId,
                name = qrData.name,
                ip = qrData.ip,
                port = qrData.port,
                pairingToken = qrData.pairingToken,
                platform = qrData.platform,
                isSelected = true
            )

            viewModelScope.launch {
                tvDao.insertOrUpdateTv(pairedTv)
                tvDao.selectTv(tvId)
                senderClient.sendPairingRequest(pairedTv)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun addManualTv(name: String, ip: String, port: Int, token: String) {
        val tvId = UUID.nameUUIDFromBytes("$ip:$port".toByteArray()).toString()
        val tv = PairedTv(
            id = tvId,
            name = if (name.isNotBlank()) name else "TV ($ip)",
            ip = ip,
            port = port,
            pairingToken = if (token.isNotBlank()) token else "demo-token",
            platform = "manual",
            isSelected = true
        )
        viewModelScope.launch {
            tvDao.insertOrUpdateTv(tv)
            tvDao.selectTv(tvId)
            senderClient.sendPairingRequest(tv)
        }
    }

    fun selectTv(tvId: String) {
        viewModelScope.launch {
            tvDao.selectTv(tvId)
        }
    }

    fun deleteTv(tv: PairedTv) {
        viewModelScope.launch {
            tvDao.deleteTv(tv)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyDao.deleteHistoryItem(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyDao.clearHistory()
        }
    }

    fun toggleTheme() {
        isDarkThemeOverride.value = when (isDarkThemeOverride.value) {
            true -> false
            false -> null
            null -> true
        }
    }

    private fun detectMediaTypeFromMime(mimeType: String?): MediaType {
        if (mimeType == null) return MediaType.VIDEO
        return when {
            mimeType.startsWith("image/") -> MediaType.IMAGE
            mimeType.startsWith("audio/") -> MediaType.AUDIO
            mimeType.startsWith("video/") -> MediaType.VIDEO
            mimeType.contains("mpegurl") || mimeType.contains("hls") -> MediaType.STREAM
            else -> MediaType.VIDEO
        }
    }

    private fun deriveTitleFromUrl(url: String): String {
        if (url.isBlank()) return ""
        return try {
            val uri = Uri.parse(url)
            val lastSegment = uri.lastPathSegment
            val host = uri.host

            if (!lastSegment.isNullOrBlank() && lastSegment.length > 2 && lastSegment.contains(".")) {
                lastSegment
            } else if (!host.isNullOrBlank()) {
                host
            } else if (!lastSegment.isNullOrBlank()) {
                lastSegment
            } else {
                url
            }
        } catch (e: Exception) {
            url.substringAfterLast("/").substringBefore("?")
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
}
