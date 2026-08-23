package com.twotv.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twotv.app.data.model.MediaType
import com.twotv.app.data.model.PairedTv
import com.twotv.app.ui.MainViewModel
import com.twotv.app.ui.SendCategoryMode
import com.twotv.app.ui.SendUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenQrScanner: () -> Unit,
    onOpenTvsScreen: () -> Unit
) {
    val selectedTv by viewModel.selectedTv.collectAsState()
    val sendMode by viewModel.sendCategoryMode.collectAsState()
    val url by viewModel.inputUrl.collectAsState()
    val title by viewModel.inputTitle.collectAsState()
    val mediaType by viewModel.selectedMediaType.collectAsState()
    val saveToTv by viewModel.saveToTv.collectAsState()
    val sendState by viewModel.sendUiState.collectAsState()
    val selectedFileUri by viewModel.selectedFileUri.collectAsState()

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val type = contentResolver.getType(uri)
            viewModel.setFileUri(uri, type)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active TV Header Card
        ActiveTvCard(
            selectedTv = selectedTv,
            onOpenQrScanner = onOpenQrScanner,
            onOpenTvsScreen = onOpenTvsScreen
        )

        // Status Banner if any
        when (val state = sendState) {
            is SendUiState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.resetSendStatus() }) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi")
                        }
                    }
                }
            }
            is SendUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(state.errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.resetSendStatus() }) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi")
                        }
                    }
                }
            }
            else -> {}
        }

        // 3-Way Mode Segmented Tabs (FILE, STREAM, LINK)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Modalità di Invio 2TV",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = sendMode == SendCategoryMode.STREAM,
                        onClick = { viewModel.setSendCategoryMode(SendCategoryMode.STREAM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stream")
                        }
                    }

                    SegmentedButton(
                        selected = sendMode == SendCategoryMode.FILE,
                        onClick = { viewModel.setSendCategoryMode(SendCategoryMode.FILE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("File")
                        }
                    }

                    SegmentedButton(
                        selected = sendMode == SendCategoryMode.LINK,
                        onClick = { viewModel.setSendCategoryMode(SendCategoryMode.LINK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Link Web")
                        }
                    }
                }
            }
        }

        // Main Input Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (sendMode) {
                    SendCategoryMode.FILE -> {
                        Text(
                            text = "Seleziona File dal Dispositivo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Il file verrà trasferito via Wi-Fi alla TV ed avviato al termine del caricamento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Scegli Video, Foto o Audio", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        if (selectedFileUri != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("File pronto per l'upload", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    SendCategoryMode.STREAM -> {
                        Text(
                            text = "Invia Stream Video / Audio (es. Stremio, HLS)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = url,
                            onValueChange = { viewModel.setUrl(it) },
                            label = { Text("URL dello Stream (.m3u8, .mp4, Stremio)") },
                            placeholder = { Text("https://example.com/stream.m3u8") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (url.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setUrl("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.LiveTv, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    SendCategoryMode.LINK -> {
                        Text(
                            text = "Invia Link Web o Download",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Se il link è una pagina web verrà aperta nel browser TV; se è un file verrà scaricato sulla TV.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = url,
                            onValueChange = { viewModel.setUrl(it) },
                            label = { Text("URL Pagina Web o File Download") },
                            placeholder = { Text("https://wikipedia.org") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Optional Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.setTitle(it) },
                    label = { Text("Titolo (Opzionale)") },
                    placeholder = { Text("es. Trailer Film / Nome Contenuto") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Salva su TV Switch
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Salva nell'Archivio TV", style = MaterialTheme.typography.titleSmall)
                                Text("Salva il contenuto nella cronologia TV", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = saveToTv,
                            onCheckedChange = { viewModel.setSaveToTv(it) }
                        )
                    }
                }

                // Big 2TV Action Button
                Button(
                    onClick = { viewModel.sendCurrentContent() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = sendState !is SendUiState.Sending && selectedTv != null && (url.isNotBlank() || selectedFileUri != null)
                ) {
                    if (sendState is SendUiState.Sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Invio in corso...")
                    } else {
                        Icon(Icons.Default.Tv, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (sendMode) {
                                SendCategoryMode.FILE -> "Carica File 2TV"
                                SendCategoryMode.STREAM -> "Avvia Stream 2TV"
                                SendCategoryMode.LINK -> "Invia Link 2TV"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Quick Sample Media Presets Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Esempi di Prova Rapida",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                SamplePresetItem(
                    title = "Video Stream MP4 (Big Buck Bunny)",
                    subtitle = "Stream Video Direct",
                    type = MediaType.VIDEO,
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    onSelect = { url, type, title ->
                        viewModel.setSendCategoryMode(SendCategoryMode.STREAM)
                        viewModel.setUrl(url)
                        viewModel.setMediaType(type)
                        viewModel.setTitle(title)
                    }
                )

                SamplePresetItem(
                    title = "Live Stream HLS (.m3u8)",
                    subtitle = "Stream Live HLS Mux",
                    type = MediaType.STREAM,
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    onSelect = { url, type, title ->
                        viewModel.setSendCategoryMode(SendCategoryMode.STREAM)
                        viewModel.setUrl(url)
                        viewModel.setMediaType(type)
                        viewModel.setTitle(title)
                    }
                )

                SamplePresetItem(
                    title = "Pagina Web Wikipedia",
                    subtitle = "Link Web da aprire nel browser TV",
                    type = MediaType.IMAGE,
                    url = "https://it.wikipedia.org",
                    onSelect = { url, _, title ->
                        viewModel.setSendCategoryMode(SendCategoryMode.LINK)
                        viewModel.setUrl(url)
                        viewModel.setTitle(title)
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveTvCard(
    selectedTv: PairedTv?,
    onOpenQrScanner: () -> Unit,
    onOpenTvsScreen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectedTv != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTv != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    if (selectedTv != null) {
                        Text(
                            text = selectedTv.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${selectedTv.ip}:${selectedTv.port} • ${selectedTv.platform}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Nessuna TV Abbinata",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Scansiona il QR Code sulla TV",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onOpenQrScanner) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scansiona QR Code")
                }
                IconButton(onClick = onOpenTvsScreen) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Cambia TV")
                }
            }
        }
    }
}

@Composable
private fun SamplePresetItem(
    title: String,
    subtitle: String,
    type: MediaType,
    url: String,
    onSelect: (url: String, type: MediaType, title: String) -> Unit
) {
    Surface(
        onClick = { onSelect(url, type, title) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (type) {
                    MediaType.VIDEO -> Icons.Default.Movie
                    MediaType.IMAGE -> Icons.Default.Image
                    MediaType.AUDIO -> Icons.Default.MusicNote
                    MediaType.STREAM -> Icons.Default.LiveTv
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
