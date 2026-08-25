package com.twotv.tv.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.twotv.tv.R
import com.twotv.tv.databinding.ActivityTvMainBinding
import com.twotv.tv.model.MediaCategory
import com.twotv.tv.model.TvArchiveItem
import com.twotv.tv.server.DevicePairInfo
import com.twotv.tv.server.TvEmbeddedServer
import com.twotv.tv.server.TvPlayPayload
import com.twotv.tv.ui.components.ImagePreviewDialog
import com.twotv.tv.ui.components.PdfPreviewDialog
import com.twotv.tv.ui.components.WebPreviewDialog
import com.twotv.tv.util.PairingManager
import com.twotv.tv.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

class TvMainActivity : FragmentActivity() {

    private lateinit var binding: ActivityTvMainBinding
    private var embeddedServer: TvEmbeddedServer? = null
    private var exoPlayer: ExoPlayer? = null

    private val archiveList = mutableListOf<TvArchiveItem>()
    private val pairedDevices = mutableListOf<DevicePairInfo>()
    private val PAIRING_TOKEN = "2tv-secret-tv-token"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load saved paired devices from SharedPreferences
        pairedDevices.clear()
        pairedDevices.addAll(PairingManager.getPairedDevices(this))
        if (pairedDevices.isNotEmpty()) {
            binding.qrCard.visibility = View.GONE
        }


        // Setup bottom control bar buttons
        binding.btnManagePairings.setOnClickListener {
            showManagePairingsDialog()
        }

        binding.btnShowHistory.setOnClickListener {
            showHistoryDialog()
        }

        binding.btnOpenWithMedia.setOnClickListener {
            if (currentPlayingItemUrl.isNotBlank()) {
                openMediaWithIntentChooser(currentPlayingItemUrl)
            }
        }

        binding.btnCloseVideo.setOnClickListener {
            stopPlaybackAndShowHome()
        }




        // Initialize components asynchronously in background thread
        lifecycleScope.launch(Dispatchers.IO) {
            val ip = getLocalIpAddress()

            val qrJson = """
                {
                  "name": "Android TV (2TV)",
                  "ip": "$ip",
                  "port": 8080,
                  "pairingToken": "$PAIRING_TOKEN",
                  "platform": "androidtv"
                }
            """.trimIndent()

            val qrBitmap = QrCodeGenerator.generateQrBitmap(qrJson, 360, 360)

            withContext(Dispatchers.Main) {
                // Initialize ExoPlayer
                exoPlayer = ExoPlayer.Builder(this@TvMainActivity).build()
                binding.playerView.player = exoPlayer

                binding.ipTextView.text = "IP TV: http://$ip:8080"
                if (qrBitmap != null) {
                    binding.qrImageView.setImageBitmap(qrBitmap)
                }
            }

            // Check for App Updates from GitHub Releases
            val updateInfo = com.twotv.tv.util.AppUpdater.checkForUpdate(
                currentVersionName = com.twotv.tv.BuildConfig.VERSION_NAME,
                isTv = true
            )
            if (updateInfo != null) {
                withContext(Dispatchers.Main) {
                    showUpdateAvailableTvDialog(updateInfo)
                }
            }

            // Start Ktor Embedded Server
            embeddedServer = TvEmbeddedServer(
                context = applicationContext,
                port = 8080,
                pairingToken = PAIRING_TOKEN,
                onPlayMedia = { payload ->
                    runOnUiThread {
                        handleReceivedMedia(payload)
                    }
                },
                onDevicePaired = { device, isSilent ->
                    runOnUiThread {
                        handleDevicePaired(device, isSilent)
                    }
                },

                onUploadProgress = { title, percentage ->
                    runOnUiThread {
                        binding.uploadProgressCard.visibility = View.VISIBLE
                        binding.uploadProgressCard.bringToFront()
                        binding.uploadTitleText.text = "Ricezione: $title"
                        binding.uploadProgressBar.progress = percentage
                        binding.uploadPercentText.text = "$percentage%"
                    }
                },
                onUploadFinished = {
                    runOnUiThread {
                        binding.uploadProgressCard.visibility = View.GONE
                    }
                }
            )
            embeddedServer?.start()
        }
    }

    private fun handleDevicePaired(device: DevicePairInfo, isSilent: Boolean = false) {
        PairingManager.saveDevice(this, device)
        pairedDevices.clear()
        pairedDevices.addAll(PairingManager.getPairedDevices(this))

        binding.qrCard.visibility = View.GONE
        if (!isSilent) {
            Toast.makeText(this, "Dispositivo connesso: ${device.deviceName} (${device.deviceIp})", Toast.LENGTH_SHORT).show()
        }
    }


    private fun toggleQrCodeVisibility() {
        if (binding.qrCard.visibility == View.VISIBLE) {
            binding.qrCard.visibility = View.GONE
        } else {
            binding.qrCard.visibility = View.VISIBLE
            binding.qrCard.bringToFront()
        }
    }

    private fun showUpdateAvailableTvDialog(updateInfo: com.twotv.tv.util.UpdateInfo) {
        AlertDialog.Builder(this)
            .setTitle("Aggiornamento Disponibile (v${updateInfo.latestVersionName})")
            .setMessage("${updateInfo.releaseNotes}\n\nDesideri scaricare ed installare subito l'aggiornamento?")
            .setPositiveButton("Aggiorna Ora") { dialog, _ ->
                dialog.dismiss()
                downloadAndInstallTvUpdate(updateInfo.downloadUrl)
            }
            .setNegativeButton("Più Tardi", null)
            .show()
    }

    private fun downloadAndInstallTvUpdate(downloadUrl: String) {
        binding.uploadProgressCard.visibility = View.VISIBLE
        binding.uploadProgressCard.bringToFront()
        binding.uploadTitleText.text = "Download Aggiornamento 2TV..."


        lifecycleScope.launch {
            val result = com.twotv.tv.util.AppUpdater.downloadAndInstallApk(
                context = this@TvMainActivity,
                downloadUrl = downloadUrl,
                authority = "com.twotv.tv.fileprovider",
                onProgress = { percent ->
                    binding.uploadProgressBar.progress = percent
                    binding.uploadPercentText.text = "$percent%"
                }
            )

            binding.uploadProgressCard.visibility = View.GONE

            if (result.isFailure) {
                Toast.makeText(
                    this@TvMainActivity,
                    "Errore download aggiornamento: ${result.exceptionOrNull()?.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showManagePairingsDialog() {
        val itemsText = if (pairedDevices.isEmpty()) {
            arrayOf("Nessun dispositivo accoppiato salvato")
        } else {
            pairedDevices.map { "• ${it.deviceName} (${it.deviceIp}) - Connesso" }.toTypedArray()
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.paired_devices_title))
            .setItems(itemsText) { _, index ->
                if (pairedDevices.isNotEmpty()) {
                    showUnpairConfirmDialog(index)
                }
            }
            .setPositiveButton(getString(R.string.btn_qr_code)) { dialog, _ ->
                binding.qrCard.visibility = View.VISIBLE
                binding.qrCard.bringToFront()
                dialog.dismiss()
            }
            .setNegativeButton("Chiudi", null)
            .show()
    }

    private fun showUnpairConfirmDialog(index: Int) {
        val device = pairedDevices[index]
        AlertDialog.Builder(this)
            .setTitle("Disaccoppia Dispositivo")
            .setMessage("Rimuovere l'accoppiamento con ${device.deviceName} (${device.deviceIp})?")
            .setPositiveButton("Rimuovi") { _, _ ->
                PairingManager.removeDevice(this, index)
                pairedDevices.clear()
                pairedDevices.addAll(PairingManager.getPairedDevices(this))
                Toast.makeText(this, "Accoppiamento rimosso", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun showHistoryDialog() {
        if (archiveList.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.archive_title))
                .setMessage(getString(R.string.no_history))
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val options = archiveList.map { item ->
            val typeStr = when (item.category) {
                MediaCategory.STREAM -> "[STREAM]"
                MediaCategory.WEB -> "[LINK]"
                MediaCategory.FILE -> "[FILE]"
            }
            "$typeStr ${item.title}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.archive_title))
            .setItems(options) { _, index ->
                showHistoryItemOptionsDialog(index)
            }
            .setNegativeButton("Chiudi", null)
            .show()
    }

    private var currentPlayingItemUrl: String = ""

    private fun showHistoryItemOptionsDialog(index: Int) {
        val item = archiveList[index]
        val actionText = if (item.category == MediaCategory.STREAM) {
            getString(R.string.btn_play)
        } else {
            getString(R.string.btn_open)
        }

        val choices = arrayOf(actionText, "Apri con... (App Esterne)", getString(R.string.btn_delete))

        AlertDialog.Builder(this)
            .setTitle(item.title)
            .setItems(choices) { _, choiceIndex ->
                when (choiceIndex) {
                    0 -> openOrPlayMediaItem(item)
                    1 -> openMediaWithIntentChooser(item.pathOrUrl, item.title)
                    2 -> {
                        if (item.isLocalFile) {
                            try {
                                File(item.pathOrUrl).delete()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        archiveList.removeAt(index)
                        Toast.makeText(this, "Elemento eliminato dalla cronologia", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun openMediaWithIntentChooser(pathOrUrl: String, title: String = "") {
        try {
            val file = File(pathOrUrl)
            val intent = if (file.exists()) {
                val uri = FileProvider.getUriForFile(this, "com.twotv.tv.fileprovider", file)
                val ext = file.extension.lowercase()
                val mimeType = getMimeTypeFromExtension(ext)
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                var validUrl = pathOrUrl
                if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
                    validUrl = "https://$validUrl"
                }
                Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val chooser = Intent.createChooser(intent, "Apri con...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossibile aprire con app esterne: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }


    private fun handleReceivedMedia(payload: TvPlayPayload) {
        val category = TvArchiveItem.categorize(payload.url, payload.mediaType)
        val isLocal = File(payload.url).exists()

        val archiveItem = TvArchiveItem(
            title = payload.title,
            pathOrUrl = payload.url,
            category = category,
            isLocalFile = isLocal
        )

        archiveList.add(0, archiveItem)
        openOrPlayMediaItem(archiveItem)
    }

    private var activePreviewDialog: Dialog? = null
    private val hideVideoHeaderRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!binding.btnOpenWithMedia.hasFocus() && !binding.btnCloseVideo.hasFocus()) {
                binding.nowPlayingBanner.visibility = View.GONE
            } else {
                binding.nowPlayingBanner.postDelayed(this, 3000)
            }
        }
    }


    private fun scheduleVideoHeaderAutoHide(requestFocusOnButton: Boolean = false) {
        binding.nowPlayingBanner.visibility = View.VISIBLE
        binding.nowPlayingBanner.bringToFront()
        binding.nowPlayingBanner.removeCallbacks(hideVideoHeaderRunnable)
        binding.nowPlayingBanner.postDelayed(hideVideoHeaderRunnable, 3000)

        if (requestFocusOnButton) {
            binding.btnOpenWithMedia.post {
                binding.btnOpenWithMedia.requestFocus()
            }
        }
    }


    private fun openOrPlayMediaItem(item: TvArchiveItem) {
        stopPlaybackAndShowHome()
        currentPlayingItemUrl = item.pathOrUrl

        binding.nowPlayingTitle.text = item.title

        when (item.category) {
            MediaCategory.STREAM -> {
                binding.mediaTypeBadge.text = "STREAM"
                binding.mediaTypeBadge.setBackgroundColor(android.graphics.Color.parseColor("#8B5CF6"))
            }
            MediaCategory.WEB -> {
                binding.mediaTypeBadge.text = "LINK"
                binding.mediaTypeBadge.setBackgroundColor(android.graphics.Color.parseColor("#0284C7"))
            }
            else -> {
                binding.mediaTypeBadge.text = "FILE"
                binding.mediaTypeBadge.setBackgroundColor(android.graphics.Color.parseColor("#10B981"))
            }
        }

        when (item.category) {
            // STREAM: Played natively inside 2TV ExoPlayer
            MediaCategory.STREAM -> {
                binding.bottomControlBar.visibility = View.GONE
                binding.idleContainer.visibility = View.GONE
                binding.playerView.visibility = View.VISIBLE
                binding.playerView.bringToFront()
                scheduleVideoHeaderAutoHide()
                val mediaItem = MediaItem.fromUri(item.pathOrUrl)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            }

            // WEB LINK: Opened in WebPreviewDialog directly inside 2TV (fallback to external browser)
            MediaCategory.WEB -> {
                try {
                    val dialog = WebPreviewDialog(this, item.title, item.pathOrUrl)
                    activePreviewDialog = dialog
                    dialog.show()
                } catch (e: Exception) {
                    openWebLinkWithDefaultBrowser(item.pathOrUrl)
                }
            }

            // FILE: Opened using built-in Previewer (PDF, Image, Video/Audio ExoPlayer) directly inside 2TV
            MediaCategory.FILE -> {
                val ext = item.pathOrUrl.substringAfterLast(".", "").lowercase()
                when (ext) {
                    "pdf" -> {
                        val file = File(item.pathOrUrl)
                        if (file.exists()) {
                            val dialog = PdfPreviewDialog(this, item.title, file)
                            activePreviewDialog = dialog
                            dialog.show()
                        } else {
                            openFileWithDefaultApp(item.pathOrUrl)
                        }
                    }
                    "jpg", "jpeg", "png", "webp", "gif", "bmp" -> {
                        val dialog = ImagePreviewDialog(this, item.title, item.pathOrUrl)
                        activePreviewDialog = dialog
                        dialog.show()
                    }
                    "mp4", "mkv", "avi", "mov", "webm", "mp3", "wav", "flac", "aac", "ogg" -> {
                        binding.bottomControlBar.visibility = View.GONE
                        binding.idleContainer.visibility = View.GONE
                        binding.playerView.visibility = View.VISIBLE
                        binding.playerView.bringToFront()
                        scheduleVideoHeaderAutoHide()
                        val file = File(item.pathOrUrl)
                        val mediaItem = if (file.exists()) {
                            MediaItem.fromUri(Uri.fromFile(file))
                        } else {
                            MediaItem.fromUri(item.pathOrUrl)
                        }
                        exoPlayer?.setMediaItem(mediaItem)
                        exoPlayer?.prepare()
                        exoPlayer?.play()
                    }
                    else -> {
                        openFileWithDefaultApp(item.pathOrUrl)
                    }
                }
            }
        }
    }


    private fun openWebLinkWithDefaultBrowser(urlStr: String) {
        try {
            var validUrl = urlStr
            if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
                validUrl = "https://$validUrl"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossibile aprire il browser predefinito: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openFileWithDefaultApp(pathOrUrl: String) {
        try {
            val file = File(pathOrUrl)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(this, "com.twotv.tv.fileprovider", file)
                val ext = file.extension.lowercase()
                val mimeType = getMimeTypeFromExtension(ext)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Nessuna app predefinita per aprire il file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeTypeFromExtension(ext: String): String = when (ext.lowercase()) {
        "mp4", "mkv", "avi", "mov", "webm" -> "video/*"
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "image/*"
        "mp3", "aac", "wav", "flac", "ogg" -> "audio/*"
        "pdf" -> "application/pdf"
        "apk" -> "application/vnd.android.package-archive"
        else -> "*/*"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (binding.playerView.visibility == View.VISIBLE) {
            val isHeaderFocused = binding.btnOpenWithMedia.hasFocus() || binding.btnCloseVideo.hasFocus()
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (isHeaderFocused) {
                        binding.nowPlayingBanner.removeCallbacks(hideVideoHeaderRunnable)
                        binding.nowPlayingBanner.visibility = View.GONE
                        binding.playerView.requestFocus()
                        return true
                    } else {
                        stopPlaybackAndShowHome()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    scheduleVideoHeaderAutoHide(requestFocusOnButton = true)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (isHeaderFocused) {
                        binding.nowPlayingBanner.removeCallbacks(hideVideoHeaderRunnable)
                        binding.nowPlayingBanner.visibility = View.GONE
                        binding.playerView.requestFocus()
                        return true
                    } else {
                        scheduleVideoHeaderAutoHide()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    scheduleVideoHeaderAutoHide()
                    if (!isHeaderFocused && binding.nowPlayingBanner.visibility == View.VISIBLE) {
                        binding.btnOpenWithMedia.requestFocus()
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun stopPlaybackAndShowHome() {
        binding.nowPlayingBanner.removeCallbacks(hideVideoHeaderRunnable)
        binding.nowPlayingBanner.visibility = View.GONE

        activePreviewDialog?.let {
            if (it.isShowing) {
                try { it.dismiss() } catch (e: Exception) { e.printStackTrace() }
            }
        }
        activePreviewDialog = null

        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        binding.playerView.visibility = View.GONE
        binding.idleContainer.visibility = View.VISIBLE
        binding.bottomControlBar.visibility = View.VISIBLE
    }


    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    override fun onDestroy() {
        super.onDestroy()
        embeddedServer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
