package com.twotv.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twotv.app.data.model.MediaType
import com.twotv.app.data.model.PairedTv
import com.twotv.app.ui.MainViewModel
import com.twotv.app.ui.SendUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenQrScanner: () -> Unit,
    onOpenTvsScreen: () -> Unit
) {
    val selectedTv by viewModel.selectedTv.collectAsState()
    val url by viewModel.inputUrl.collectAsState()
    val title by viewModel.inputTitle.collectAsState()
    val mediaType by viewModel.selectedMediaType.collectAsState()
    val saveToTv by viewModel.saveToTv.collectAsState()
    val sendState by viewModel.sendUiState.collectAsState()

    val scrollState = rememberScrollState()

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

        // Send Status Banner if any
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

        // Input Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Condividi Contenuto",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // URL Input
                OutlinedTextField(
                    value = url,
                    onValueChange = { viewModel.setUrl(it) },
                    label = { Text("URL del file / stream") },
                    placeholder = { Text("https://example.com/video.mp4") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (url.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setUrl("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Pulisci")
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.setTitle(it) },
                    label = { Text("Titolo (Opzionale)") },
                    placeholder = { Text("es. Trailer Film") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Media Type Selector Chips
                Text(
                    text = "Tipo di Contenuto",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaType.values().forEach { type ->
                        val selected = mediaType == type
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setMediaType(type) },
                            label = { Text(type.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (type) {
                                        MediaType.VIDEO -> Icons.Default.Movie
                                        MediaType.IMAGE -> Icons.Default.Image
                                        MediaType.AUDIO -> Icons.Default.MusicNote
                                        MediaType.STREAM -> Icons.Default.LiveTv
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                // Switch Salva su TV
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
                                Text("Salva su TV", style = MaterialTheme.typography.titleSmall)
                                Text("Salva nella memoria / cronologia TV", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    enabled = sendState !is SendUiState.Sending && selectedTv != null && url.isNotBlank()
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
                            text = "Invia 2TV",
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
                    title = "Video Demopack (Big Buck Bunny)",
                    subtitle = "Video MP4",
                    type = MediaType.VIDEO,
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    onSelect = { url, type, title ->
                        viewModel.setUrl(url)
                        viewModel.setMediaType(type)
                        viewModel.setTitle(title)
                    }
                )

                SamplePresetItem(
                    title = "Live Stream HLS (TVI Test Stream)",
                    subtitle = "Stream Live HLS .m3u8",
                    type = MediaType.STREAM,
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    onSelect = { url, type, title ->
                        viewModel.setUrl(url)
                        viewModel.setMediaType(type)
                        viewModel.setTitle(title)
                    }
                )

                SamplePresetItem(
                    title = "Foto 4K Wallpaper (Unsplash)",
                    subtitle = "Immagine JPG High-Res",
                    type = MediaType.IMAGE,
                    url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1920&q=80",
                    onSelect = { url, type, title ->
                        viewModel.setUrl(url)
                        viewModel.setMediaType(type)
                        viewModel.setTitle(title)
                    }
                )

                SamplePresetItem(
                    title = "Audio Podcast Sample",
                    subtitle = "Audio MP3",
                    type = MediaType.AUDIO,
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    onSelect = { url, type, title ->
                        viewModel.setUrl(url)
                        viewModel.setMediaType(type)
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
