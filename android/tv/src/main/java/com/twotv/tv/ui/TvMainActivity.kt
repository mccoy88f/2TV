package com.twotv.tv.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.load
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
import java.net.URL

class TvMainActivity : FragmentActivity() {

    private lateinit var binding: ActivityTvMainBinding
    private var embeddedServer: TvEmbeddedServer? = null
    private var exoPlayer: ExoPlayer? = null

    private val archiveList = mutableListOf<TvArchiveItem>()
    private val pairedDevices = mutableSetOf<String>()
    private val PAIRING_TOKEN = "2tv-secret-tv-token"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer

        // Initialize WebView for TV Web Pages
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.allowFileAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
            }
        }

        // Get IP and generate QR Code
        val ip = getLocalIpAddress()
        binding.ipTextView.text = "IP TV: http://$ip:8080"

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
        if (qrBitmap != null) {
            binding.qrImageView.setImageBitmap(qrBitmap)
        }

        binding.btnShowQrCode.setOnClickListener {
            showQrCodeScreen()
        }

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
            }
        )
        embeddedServer?.start()
    }

    private fun handleDevicePaired(device: DevicePairInfo) {
        val entry = "• ${device.deviceName} (${device.deviceIp})"
        pairedDevices.add(entry)

        binding.qrCard.visibility = View.GONE
        binding.pairedDevicesCard.visibility = View.VISIBLE
        binding.pairedDevicesListText.text = pairedDevices.joinToString("\n")
    }

    private fun showQrCodeScreen() {
        binding.pairedDevicesCard.visibility = View.GONE
        binding.qrCard.visibility = View.VISIBLE
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

        if (payload.saveToTv || isLocal) {
            archiveList.add(0, archiveItem)
        }

        playItem(archiveItem)
    }

    private fun playItem(item: TvArchiveItem) {
        binding.nowPlayingBanner.visibility = View.VISIBLE
        binding.nowPlayingTitle.text = item.title
        binding.nowPlayingUrl.text = item.pathOrUrl
        binding.mediaTypeBadge.text = item.category.name

        binding.idleContainer.visibility = View.GONE
        binding.playerView.visibility = View.GONE
        binding.imageView.visibility = View.GONE
        binding.webView.visibility = View.GONE

        when (item.category) {
            MediaCategory.FOTO -> {
                binding.imageView.visibility = View.VISIBLE
                binding.imageView.bringToFront()
                if (item.isLocalFile) {
                    binding.imageView.load(File(item.pathOrUrl))
                } else {
                    binding.imageView.load(item.pathOrUrl)
                }
            }

            MediaCategory.VIDEO, MediaCategory.AUDIO -> {
                binding.playerView.visibility = View.VISIBLE
                binding.playerView.bringToFront()
                binding.playerView.requestFocus()
                val mediaItem = MediaItem.fromUri(item.pathOrUrl)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            }

            MediaCategory.WEB -> {
                binding.webView.visibility = View.VISIBLE
                binding.webView.bringToFront()
                binding.webView.requestFocus()
                var validUrl = item.pathOrUrl
                if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
                    validUrl = "https://$validUrl"
                }
                binding.webView.loadUrl(validUrl)
            }

            MediaCategory.ALTRO -> {
                if (item.pathOrUrl.startsWith("http://") || item.pathOrUrl.startsWith("https://")) {
                    downloadFileToTv(item.pathOrUrl, item.title)
                } else {
                    Toast.makeText(this, "File Locale TV: ${item.pathOrUrl}", Toast.LENGTH_LONG).show()
                }
                binding.idleContainer.visibility = View.VISIBLE
            }
        }

        binding.root.postDelayed({
            binding.nowPlayingBanner.visibility = View.GONE
        }, 5000)
    }

    private fun downloadFileToTv(urlStr: String, title: String) {
        Toast.makeText(this, "Download avviato per: $title", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val downloadsDir = File(filesDir, "downloads").apply { mkdirs() }
                val fileName = title.ifBlank { urlStr.substringAfterLast("/") }.substringBefore("?")
                val destFile = File(downloadsDir, fileName.ifBlank { "downloaded_file_${System.currentTimeMillis()}" })

                URL(urlStr).openStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TvMainActivity, "Download completato: ${destFile.name}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TvMainActivity, "Errore download: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.idleContainer.visibility != View.VISIBLE) {
            stopPlaybackAndShowHome()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun stopPlaybackAndShowHome() {
        exoPlayer?.stop()
        binding.playerView.visibility = View.GONE
        binding.imageView.visibility = View.GONE
        binding.webView.visibility = View.GONE
        binding.idleContainer.visibility = View.VISIBLE
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
