package com.example.warehousescan.feature.locationsetting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.warehousescan.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSettingScreen(
    onBack: () -> Unit,
    viewModel: LocationSettingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannerStatus by viewModel.scannerStatus.collectAsState()
    val pendingChallans by viewModel.pendingChallans.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showPendingChallansDialog by remember { mutableStateOf(false) }
    var showBackConfirmDialog by remember { mutableStateOf(false) }
    val hostView = LocalView.current
    LifecycleResumeEffect(viewModel, hostView) {
        viewModel.onScreenResumed()
        hostView.keepScreenOn = true
        onPauseOrDispose {
            viewModel.onScreenPaused()
            hostView.keepScreenOn = false
        }
    }

    val currentBatches = (uiState as? LocationSettingUiState.Content)?.batches.orEmpty()
    val isSubmitted = (uiState as? LocationSettingUiState.Content)?.sessionSubmitted == true

    BackHandler(enabled = currentBatches.isNotEmpty() && !isSubmitted) {
        showBackConfirmDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Setting") },
                navigationIcon = {
                    TextButton(onClick = {
                        if (currentBatches.isNotEmpty() && !isSubmitted) {
                            showBackConfirmDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showPendingChallansDialog = true }) {
                        Text("Pending (${pendingChallans.size})")
                    }
                    TextButton(onClick = { showClearDialog = true }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is LocationSettingUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Location", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactLocationField("Rack", state.rack, viewModel::updateRack, Modifier.weight(1f))
                                CompactLocationField("Zone", state.zone, viewModel::updateZone, Modifier.weight(1f))
                                CompactLocationField("Setting", state.setting, viewModel::updateSetting, Modifier.weight(1f))
                            }
                        }
                    }

                    ScannerStatusPanel(scannerStatus)

                    Spacer(modifier = Modifier.height(12.dp))

                    ScanInputBar(
                        value = state.manualInput,
                        onValueChange = { viewModel.updateManualInput(it) },
                        onAddClick = { viewModel.addBatch(state.manualInput) },
                        onScanClick = {
                            if (scannerStatus.dataWedgePresent) {
                                viewModel.triggerScan()
                            } else {
                                showCameraScanner = true
                            }
                        },
                        onCameraScanClick = { showCameraScanner = true },
                        scanEnabled = true,
                        addEnabled = true
                    )

                    if (!state.errorMessage.isNullOrBlank()) {
                        ErrorBanner(message = state.errorMessage!!) {
                            // dismiss
                        }
                    }

                    if (!state.successMessage.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = state.successMessage!!,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Batches Scanned: ${state.batches.size}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(state.batches.asReversed()) { reverseIndex, batch ->
                            val originalIndex = state.batches.lastIndex - reverseIndex
                            BatchListItem(
                                batchNumber = batch,
                                index = originalIndex + 1,
                                onDelete = { viewModel.removeBatch(originalIndex) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.saveAndHoldChallan() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text("Hold / Save", fontSize = 15.sp)
                        }

                        LoadingButton(
                            text = "Submit Session",
                            isLoading = state.isSubmitting,
                            onClick = { viewModel.submit() },
                            modifier = Modifier.weight(1f),
                            enabled = !state.sessionSubmitted || state.showEmailConfirmation
                        )
                    }
                }

                if (state.showEmailConfirmation) {
                    AlertDialog(
                        onDismissRequest = { viewModel.confirmEmailSent(false) },
                        title = { Text("Email Confirmation") },
                        text = { Text("Did you successfully send the email?") },
                        confirmButton = {
                            Button(onClick = { viewModel.confirmEmailSent(true) }) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { viewModel.confirmEmailSent(false) }) {
                                Text("No / Retry")
                            }
                        }
                    )
                }
            }
        }

        if (showClearDialog) {
            ConfirmDialog(
                title = "Clear All Batches?",
                message = "Are you sure you want to remove all scanned batches in this session?",
                onConfirm = {
                    viewModel.clearAll()
                    showClearDialog = false
                },
                onDismiss = { showClearDialog = false }
            )
        }

        if (showCameraScanner) {
            CameraScanDialog(
                onDismissRequest = { showCameraScanner = false },
                onBarcodeScanned = { scannedCode, symbology ->
                    viewModel.processCameraScan(scannedCode, symbology)
                    showCameraScanner = false
                }
            )
        }

        if (showPendingChallansDialog) {
            AlertDialog(
                onDismissRequest = { showPendingChallansDialog = false },
                title = { Text("Saved / Pending Challans") },
                text = {
                    if (pendingChallans.isEmpty()) {
                        Text("No pending saved challans.", modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                            items(pendingChallans.size) { index ->
                                val challan = pendingChallans[index]
                                val count = runCatching {
                                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(challan.batchesJson).size
                                }.getOrDefault(0)
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${challan.rack} - ${challan.zone} - ${challan.setting}", style = MaterialTheme.typography.titleMedium)
                                            Text("$count batches saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Row {
                                            TextButton(onClick = {
                                                viewModel.loadChallan(challan)
                                                showPendingChallansDialog = false
                                            }) { Text("Reopen") }
                                            IconButton(onClick = { viewModel.deleteChallan(challan.id) }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPendingChallansDialog = false }) { Text("Close") }
                }
            )
        }

        if (showBackConfirmDialog) {
            ConfirmDialog(
                title = "Discard ${currentBatches.size} scanned batches?",
                message = "You have unsaved scanned batches. Exiting now will clear these batches unless you hold/save the challan first.",
                onConfirm = {
                    viewModel.clearAll()
                    showBackConfirmDialog = false
                    onBack()
                },
                onDismiss = { showBackConfirmDialog = false }
            )
        }
    }
}

@Composable
private fun CompactLocationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 17.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                Box(contentAlignment = androidx.compose.ui.Alignment.CenterStart) {
                    if (value.isEmpty()) Text("Enter", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    innerTextField()
                }
            }
        )
    }
}
