package com.twotv.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twotv.app.R
import com.twotv.app.data.model.PairedTv
import com.twotv.app.ui.MainViewModel

@Composable
fun TvsScreen(
    viewModel: MainViewModel,
    onOpenQrScanner: () -> Unit
) {
    val pairedTvs by viewModel.pairedTvs.collectAsState()
    val selectedTv by viewModel.selectedTv.collectAsState()

    var showManualAddDialog by remember { mutableStateOf(false) }
    var editingTv by remember { mutableStateOf<PairedTv?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.tvs_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(R.string.tvs_count, pairedTvs.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = { showManualAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.manual_ip))
                }
                IconButton(onClick = onOpenQrScanner) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_qr), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Card(
            onClick = onOpenQrScanner,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pair_new_tv),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.pair_new_tv_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        if (pairedTvs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.TvOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_tv_paired),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pairedTvs, key = { it.id }) { tv ->
                    val isSelected = selectedTv?.id == tv.id
                    PairedTvItemCard(
                        tv = tv,
                        isSelected = isSelected,
                        onSelect = { viewModel.selectTv(tv.id) },
                        onEditNickname = { editingTv = tv },
                        onDelete = { viewModel.deleteTv(tv) }
                    )
                }
            }
        }
    }

    if (showManualAddDialog) {
        ManualTvAddDialog(
            onDismiss = { showManualAddDialog = false },
            onAdd = { name, ip, port, token ->
                viewModel.addManualTv(name, ip, port, token)
                showManualAddDialog = false
            }
        )
    }

    editingTv?.let { tv ->
        EditNicknameDialog(
            tv = tv,
            onDismiss = { editingTv = null },
            onSave = { newName ->
                viewModel.updateTvCustomName(tv, newName)
                editingTv = null
            }
        )
    }
}

@Composable
private fun PairedTvItemCard(
    tv: PairedTv,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEditNickname: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tv.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.active_badge), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val subText = if (!tv.customName.isNullOrBlank()) {
                    "IP: ${tv.ip}:${tv.port} • ${tv.name}"
                } else {
                    "IP: ${tv.ip}:${tv.port} (${tv.platform})"
                }
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEditNickname) {
                Icon(Icons.Default.Edit, contentDescription = "Modifica Nickname", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EditNicknameDialog(
    tv: PairedTv,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nicknameText by remember { mutableStateOf(tv.customName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nickname TV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Assegna un soprannome personalizzato a questa TV (es. TV Soggiorno, TV Camera):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nicknameText,
                    onValueChange = { nicknameText = it },
                    label = { Text("Nickname Personalizzato") },
                    placeholder = { Text(tv.name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nicknameText) }
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ManualTvAddDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, ip: String, port: Int, token: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("8080") }
    var token by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_ip)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("TV Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address (es. 192.168.1.150)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port (default 8080)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token (Optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 8080
                    if (ip.isNotBlank()) {
                        onAdd(name, ip.trim(), port, token.trim())
                    }
                },
                enabled = ip.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
