package com.anamuslim.app.ui.tasbih

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anamuslim.app.R
import com.anamuslim.app.data.tasbih.TasbihCounter

@Composable
fun TasbihScreen(viewModel: TasbihViewModel = viewModel()) {
    val counters by viewModel.counters.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageFor by remember { mutableStateOf<TasbihCounter?>(null) }
    var showResetConfirmFor by remember { mutableStateOf<TasbihCounter?>(null) }

    LaunchedEffect(counters) {
        if (selectedId == null && counters.isNotEmpty()) selectedId = counters.first().id
        if (selectedId != null && counters.none { it.id == selectedId }) {
            selectedId = counters.firstOrNull()?.id
        }
    }

    val selected = counters.firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.nav_tasbih)) },
            actions = {
                IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tasbih_add_new)) }
            }
        )

        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (selected != null) {
                Text(selected.name, style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                            )
                        )
                        .clickable { viewModel.increment(selected.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        toArabicDigits(selected.count),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row {
                    TextButton(onClick = { showManageFor = selected }) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.tasbih_edit)) }
                    Spacer(Modifier.width(16.dp))
                    TextButton(onClick = { showResetConfirmFor = selected }) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.tasbih_reset_counter)) }
                }
            } else {
                Text(stringResource(R.string.tasbih_add_new))
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(counters) { counter ->
                FilterChip(
                    selected = counter.id == selectedId,
                    onClick = { selectedId = counter.id },
                    label = { Text("${counter.name} (${toArabicDigits(counter.count)})") }
                )
            }
        }
    }

    if (showAddDialog) {
        AddTasbihDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name -> viewModel.addCustom(name); showAddDialog = false }
        )
    }

    showManageFor?.let { counter ->
        ManageTasbihDialog(
            counter = counter,
            onDismiss = { showManageFor = null },
            onRename = { newName -> viewModel.rename(counter.id, newName); showManageFor = null },
            onDelete = { viewModel.delete(counter.id); showManageFor = null }
        )
    }

    showResetConfirmFor?.let { counter ->
        ResetConfirmDialog(
            counterName = counter.name,
            onConfirm = { viewModel.resetCount(counter.id); showResetConfirmFor = null },
            onDismiss = { showResetConfirmFor = null }
        )
    }
}

@Composable
private fun AddTasbihDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tasbih_add_new)) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.tasbih_name_hint)) }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.tasbih_add_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tasbih_cancel)) } }
    )
}

@Composable
private fun ManageTasbihDialog(counter: TasbihCounter, onDismiss: () -> Unit, onRename: (String) -> Unit, onDelete: () -> Unit) {
    var text by remember { mutableStateOf(counter.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tasbih_edit)) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onRename(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.ok)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.tasbih_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.tasbih_cancel)) }
            }
        }
    )
}

@Composable
private fun ResetConfirmDialog(counterName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tasbih_reset_confirm_title)) },
        text = { Text(stringResource(R.string.tasbih_reset_confirm_message, counterName)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.tasbih_reset_yes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tasbih_reset_no)) } }
    )
}

private val arabicDigits = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
private fun toArabicDigits(n: Int): String = n.toString().map { c -> if (c.isDigit()) arabicDigits[c - '0'] else c }.joinToString("")
