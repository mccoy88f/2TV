package com.twotv.tv.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
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

        // Setup bottom control bar buttons
        binding.btnManagePairings.setOnClickListener {
            showManagePairingsDialog()
        }

        binding.btnShowHistory.setOnClickListener {
            showHistoryDialog()
        }

        binding.btnToggleQrCode.setOnClickListener {
            toggleQrCodeVisibility()
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
                onDevicePaired = { device ->
                    runOnUiThread {
                        handleDevicePaired(device)
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

    private fun handleDevicePaired(device: DevicePairInfo) {
        val existingIndex = pairedDevices.indexOfFirst { it.deviceIp == device.deviceIp }
        if (existingIndex != -1) {
            pairedDevices[existingIndex] = device
        } else {
            pairedDevices.add(0, device)
        }

        // Hide QR code upon pairing to give clean screen view
        binding.qrCard.visibility = View.GONE
        Toast.makeText(this, "Dispositivo accoppiato: ${device.deviceName} (${device.deviceIp})", Toast.LENGTH_SHORT).show()
    }

    private fun toggleQrCodeVisibility() {
        if (binding.qrCard.visibility == View.VISIBLE) {
            binding.qrCard.visibility = View.GONE
        } else {
            binding.qrCard.visibility = View.VISIBLE
            binding.qrCard.bringToFront()
        }
    }

    private fun showManagePairingsDialog() {
        val itemsText = if (pairedDevices.isEmpty()) {
            arrayOf("Nessun dispositivo accoppiato al momento")
        } else {
            pairedDevices.map { "• ${it.deviceName} (${it.deviceIp})" }.toTypedArray()
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
                pairedDevices.removeAt(index)
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
                MediaCategory.WEB -> "[LINK]"
                MediaCategory.VIDEO, MediaCategory.AUDIO -> "[STREAM]"
                else -> "[FILE]"
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

    private fun showHistoryItemOptionsDialog(index: Int) {
        val item = archiveList[index]
        val actionText = if (item.category == MediaCategory.VIDEO || item.category == MediaCategory.AUDIO) {
            getString(R.string.btn_play)
        } else {
            getString(R.string.btn_open)
        }

        val choices = arrayOf(actionText, getString(R.string.btn_delete))

        AlertDialog.Builder(this)
            .setTitle(item.title)
            .setItems(choices) { _, choiceIndex ->
                when (choiceIndex) {
                    0 -> openOrPlayMediaItem(item)
                    1 -> {
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

    private fun handleReceivedMedia(payload: TvPlayPayload) {
        val category = if (payload.mediaType.equals("WEB", ignoreCase = true)) {
            MediaCategory.WEB
        } else {
            TvArchiveItem.categorize(payload.url, payload.mediaType)
        }
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

    private fun openOrPlayMediaItem(item: TvArchiveItem) {
        binding.nowPlayingBanner.visibility = View.VISIBLE
        binding.nowPlayingTitle.text = item.title
        binding.nowPlayingUrl.text = item.pathOrUrl
        binding.mediaTypeBadge.text = item.category.name
        binding.nowPlayingBanner.bringToFront()

        when {
            // STREAM (Video or Audio stream): Play natively inside 2TV ExoPlayer
            item.category == MediaCategory.VIDEO || item.category == MediaCategory.AUDIO -> {
                binding.bottomControlBar.visibility = View.GONE // Hide bottom bar during playback!
                binding.idleContainer.visibility = View.GONE
                binding.playerView.visibility = View.VISIBLE
                binding.playerView.bringToFront()
                binding.playerView.requestFocus()
                val mediaItem = MediaItem.fromUri(item.pathOrUrl)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            }

            // WEB LINK: Open with default TV Web Browser application
            item.category == MediaCategory.WEB -> {
                stopPlaybackAndShowHome()
                openWebLinkWithDefaultBrowser(item.pathOrUrl)
            }

            // FILE (PDF, Photos, Documents, Local Files): Open with default external TV app via FileProvider
            else -> {
                stopPlaybackAndShowHome()
                openFileWithDefaultApp(item.pathOrUrl)
            }
        }

        binding.root.postDelayed({
            binding.nowPlayingBanner.visibility = View.GONE
        }, 4000)
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
                val uri: Uri = FileProvider.getUriForFile(this, "com.twotv.tv.fileprovider", file)
                val mimeType = getMimeTypeFromExtension(file.extension)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(Intent.createChooser(intent, "Apri file con..."))
            } else if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pathOrUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "File non trovato sul dispositivo: $pathOrUrl", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Nessuna app predefinita per aprire il file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.playerView.visibility == View.VISIBLE) {
            stopPlaybackAndShowHome()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun stopPlaybackAndShowHome() {
        exoPlayer?.stop()
        binding.playerView.visibility = View.GONE
        binding.idleContainer.visibility = View.VISIBLE
        binding.bottomControlBar.visibility = View.VISIBLE // Show bottom bar when back to home!
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
