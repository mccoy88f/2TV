package com.twotv.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twotv.app.R
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
    val isTvOnline by viewModel.isTvOnline.collectAsState()
    val sendMode by viewModel.sendCategoryMode.collectAsState()
    val url by viewModel.inputUrl.collectAsState()
    val title by viewModel.inputTitle.collectAsState()
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
        // Active TV Header Card with Connection Status Indicator
        ActiveTvCard(
            selectedTv = selectedTv,
            isTvOnline = isTvOnline,
            onOpenQrScanner = onOpenQrScanner,
            onOpenTvsScreen = onOpenTvsScreen,
            onRefreshConnection = { viewModel.checkConnectionStatus() }
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
                            Icon(Icons.Default.Close, contentDescription = null)
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
                            Icon(Icons.Default.Close, contentDescription = null)
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
                    text = stringResource(R.string.send_mode_title),
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
                            Text(stringResource(R.string.mode_stream))
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
                            Text(stringResource(R.string.mode_file))
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
                            Text(stringResource(R.string.mode_link))
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
                            text = stringResource(R.string.select_file_btn),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                            Text(stringResource(R.string.select_file_btn), color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text(stringResource(R.string.file_ready), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    SendCategoryMode.STREAM -> {
                        Text(
                            text = stringResource(R.string.start_stream_2tv),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = url,
                            onValueChange = { viewModel.setUrl(it) },
                            label = { Text(stringResource(R.string.url_label)) },
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
                            text = stringResource(R.string.send_link_2tv),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = url,
                            onValueChange = { viewModel.setUrl(it) },
                            label = { Text(stringResource(R.string.url_label)) },
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
                    label = { Text(stringResource(R.string.title_optional)) },
                    placeholder = { Text("es. Trailer Film") },
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
                                Text(stringResource(R.string.save_to_tv), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.save_to_tv_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("...")
                    } else {
                        Icon(Icons.Default.Tv, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (sendMode) {
                                SendCategoryMode.FILE -> stringResource(R.string.upload_file_2tv)
                                SendCategoryMode.STREAM -> stringResource(R.string.start_stream_2tv)
                                SendCategoryMode.LINK -> stringResource(R.string.send_link_2tv)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTvCard(
    selectedTv: PairedTv?,
    isTvOnline: Boolean?,
    onOpenQrScanner: () -> Unit,
    onOpenTvsScreen: () -> Unit,
    onRefreshConnection: () -> Unit
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
                            when {
                                selectedTv == null -> Color.Gray
                                isTvOnline == false -> Color(0xFFFF5252)
                                else -> Color(0xFF00E676)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = if (selectedTv != null && isTvOnline != false) Color.Black else Color.White
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
                            text = "${selectedTv.ip}:${selectedTv.port} • " + if (isTvOnline == false) "Non raggiungibile" else "Connessa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )


                    } else {
                        Text(
                            text = stringResource(R.string.no_tv_paired),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.scan_qr_to_pair),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row {
                if (selectedTv != null) {
                    IconButton(onClick = onRefreshConnection) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aggiorna Connessione")
                    }
                }
                IconButton(onClick = onOpenQrScanner) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_qr))
                }
                IconButton(onClick = onOpenTvsScreen) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.switch_tv))
                }
            }
        }
    }
}
