package com.twotv.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.twotv.app.data.model.MediaType
import com.twotv.app.data.model.PairedTv
import com.twotv.app.ui.MainViewModel
import com.twotv.app.ui.SendCategoryMode
import com.twotv.app.ui.components.QRScannerDialog

import com.twotv.app.ui.components.TvSelectionShareDialog
import com.twotv.app.ui.screens.HistoryScreen
import com.twotv.app.ui.screens.HomeScreen
import com.twotv.app.ui.screens.TvsScreen
import com.twotv.app.ui.theme.TwoTVTheme

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("2TV", Icons.Default.Tv),
    HISTORY("Cronologia", Icons.Default.History),
    TVS("TV Abbinate", Icons.Default.Devices)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isFromExternalShare by mutableStateOf(false)
        handleIncomingIntent(intent) {
            isFromExternalShare = true
        }

        setContent {
            val isDarkThemeOverride by viewModel.isDarkThemeOverride.collectAsState()
            val pairedTvs by viewModel.pairedTvs.collectAsState()
            val useDark = isDarkThemeOverride ?: isSystemInDarkTheme()

            var showTvShareDialog by remember { mutableStateOf(isFromExternalShare) }

            TwoTVTheme(darkTheme = useDark) {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                var showQrScannerDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("2TV", style = MaterialTheme.typography.titleLarge)
                                }
                            },
                            actions = {
                                IconButton(onClick = { showQrScannerDialog = true }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Abbina TV via QR")
                                }
                                IconButton(onClick = { viewModel.toggleTheme() }) {
                                    Icon(
                                        imageVector = if (useDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Cambia Tema"
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            Screen.values().forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    label = { Text(screen.title) },
                                    icon = { Icon(screen.icon, contentDescription = screen.title) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onOpenQrScanner = { showQrScannerDialog = true },
                                onOpenTvsScreen = { currentScreen = Screen.TVS }
                            )
                            Screen.HISTORY -> HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToHome = { currentScreen = Screen.HOME }
                            )
                            Screen.TVS -> TvsScreen(
                                viewModel = viewModel,
                                onOpenQrScanner = { showQrScannerDialog = true }
                            )
                        }
                    }

                    if (showQrScannerDialog) {
                        QRScannerDialog(
                            onDismiss = { showQrScannerDialog = false },
                            onQrCodeScanned = { qrJson ->
                                val success = viewModel.addPairingFromQrJson(qrJson)
                                if (success) {
                                    showQrScannerDialog = false
                                }
                            }
                        )
                    }

                    if (showTvShareDialog && pairedTvs.isNotEmpty()) {
                        TvSelectionShareDialog(
                            pairedTvs = pairedTvs,
                            onSelectTv = { selectedTv ->
                                showTvShareDialog = false
                                viewModel.sendCurrentContent(targetTv = selectedTv)
                            },
                            onDismiss = { showTvShareDialog = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent) {}
    }

    private fun handleIncomingIntent(intent: Intent?, onReceived: () -> Unit) {
        if (intent == null) return

        when (intent.action) {
            // Stremio / External Player / Web Video Caster share target
            Intent.ACTION_VIEW -> {
                val dataUri = intent.data
                val mimeType = intent.type
                if (dataUri != null) {
                    val urlString = dataUri.toString()
                    if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
                        viewModel.setUrl(urlString)
                        viewModel.setSendCategoryMode(SendCategoryMode.STREAM)
                        viewModel.setTitle(intent.getStringExtra(Intent.EXTRA_TITLE) ?: "Stream Stremio / Player")
                    } else {
                        viewModel.setFileUri(dataUri, mimeType)
                        viewModel.setSendCategoryMode(SendCategoryMode.FILE)
                    }
                    onReceived()
                }
            }

            // Android System Share
            Intent.ACTION_SEND -> {
                val type = intent.type
                if (type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    if (sharedText.isNotBlank()) {
                        val extractedUrl = extractUrl(sharedText)
                        viewModel.setUrl(extractedUrl)
                        onReceived()
                    }
                } else {
                    val fileUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    if (fileUri != null) {
                        viewModel.setFileUri(fileUri, type)
                        viewModel.setSendCategoryMode(SendCategoryMode.FILE)
                        onReceived()
                    }
                }
            }
        }
    }

    private fun extractUrl(text: String): String {
        val urlRegex = Regex("(https?://[\\w-]+(\\.[\\w-]+)+[/#?]?.*)")
        val match = urlRegex.find(text)
        return match?.value ?: text
    }
}
