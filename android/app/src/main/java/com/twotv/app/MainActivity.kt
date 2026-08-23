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
import com.twotv.app.data.model.PairedTv
import com.twotv.app.ui.MainViewModel
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

        var isFromShareIntent by mutableStateOf(false)
        handleShareIntent(intent) {
            isFromShareIntent = true
        }


        setContent {
            val isDarkThemeOverride by viewModel.isDarkThemeOverride.collectAsState()
            val pairedTvs by viewModel.pairedTvs.collectAsState()
            val useDark = isDarkThemeOverride ?: isSystemInDarkTheme()

            var showTvShareDialog by remember { mutableStateOf(isFromShareIntent) }

            TwoTVTheme(darkTheme = useDark) {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                var showQrScannerDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        OptIn(ExperimentalMaterial3Api::class)
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
        handleShareIntent(intent) {}
    }

    private fun handleShareIntent(intent: Intent?, onShared: () -> Unit) {
        if (intent?.action == Intent.ACTION_SEND) {
            val type = intent.type
            if (type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                if (sharedText.isNotBlank()) {
                    val extractedUrl = extractUrl(sharedText)
                    viewModel.setUrl(extractedUrl)
                    onShared()
                }
            } else {
                // Local Media File Uri
                val fileUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (fileUri != null) {
                    viewModel.setFileUri(fileUri, type)
                    onShared()
                }
            }
        }
    }


    private fun extractUrl(text: String): String {
        val urlRegex = Regex("(https?://[\\w-]+(\\.[\\w-]+)+[/#?]?.*)")
        val match = urlRegex.find(text)
        return match?.value ?: text
    }

    private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()
}
