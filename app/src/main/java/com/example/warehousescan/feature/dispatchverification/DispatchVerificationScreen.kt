package com.example.warehousescan.feature.dispatchverification

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehousescan.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchVerificationScreen(
    onBack: () -> Unit,
    viewModel: DispatchVerificationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannerStatus by viewModel.scannerStatus.collectAsState()
    val context = LocalContext.current
    var showCameraScanner by remember { mutableStateOf(false) }
    val hostView = LocalView.current
    LifecycleResumeEffect(viewModel, hostView) {
        viewModel.onScreenResumed()
        hostView.keepScreenOn = true
        onPauseOrDispose {
            viewModel.onScreenPaused()
            hostView.keepScreenOn = false
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.parseFile(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dispatch Verification") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    Button(onClick = { filePickerLauncher.launch("text/plain") }) {
                        Text("Upload TXT")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DispatchUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    ScannerStatusPanel(scannerStatus)
                    if (!state.isUploaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No dispatch list uploaded yet.", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { filePickerLauncher.launch("text/plain") }) {
                                    Text("Select Dispatch TXT File")
                                }
                            }
                        }
                    } else {
                        // Compact 4-Metric Row for TC26 display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CompactMetricCard("Uploaded", state.uploadedTotal, Color.DarkGray, Modifier.weight(1f)) { viewModel.setActiveTab(0) }
                            CompactMetricCard("Matched", state.matchedTotal, Color(0xFF2E7D32), Modifier.weight(1f)) { viewModel.setActiveTab(1) }
                            CompactMetricCard("Unmatched", state.unmatchedTotal, Color(0xFFC62828), Modifier.weight(1f)) { viewModel.setActiveTab(2) }
                            CompactMetricCard("Pending", state.pendingTotal, Color(0xFFEF6C00), Modifier.weight(1f)) { viewModel.setActiveTab(3) }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Scan feedback banner (Section 5.4 requirement)
                        val lastMessage = state.lastScanMessage
                        val lastStatus = state.lastScanStatus
                        
                        val bannerBg = when (lastStatus) {
                            "MATCHED" -> Color(0xFFE3F6EA)
                            "UNMATCHED" -> Color(0xFFFDE8E8)
                            "ALREADY_SCANNED" -> Color(0xFFFFF3D6)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val bannerTextColor = when (lastStatus) {
                            "MATCHED" -> Color(0xFF12873D)
                            "UNMATCHED" -> Color(0xFFC62828)
                            "ALREADY_SCANNED" -> Color(0xFFB26A00)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val bannerTitle = when (lastStatus) {
                            "MATCHED" -> "MATCH"
                            "UNMATCHED" -> "No Matching Found"
                            "ALREADY_SCANNED" -> "Already scanned"
                            else -> "Scan a roll to verify"
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = bannerBg
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = bannerTitle,
                                    color = bannerTextColor,
                                    fontSize = 24.sp,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                if (!lastMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lastMessage,
                                        color = bannerTextColor,
                                        fontSize = 14.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ScanInputBar(
                            value = state.manualInput,
                            onValueChange = { viewModel.updateManualInput(it) },
                            onAddClick = { viewModel.processScan(state.manualInput) },
                            onScanClick = {
                                if (scannerStatus.dataWedgePresent) {
                                    viewModel.triggerScan()
                                } else {
                                    showCameraScanner = true
                                }
                            },
                            onCameraScanClick = { showCameraScanner = true },
                            scanEnabled = state.isUploaded,
                            addEnabled = state.isUploaded
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tabs for inspecting lists
                        TabRow(selectedTabIndex = state.activeTab) {
                            Tab(selected = state.activeTab == 0, onClick = { viewModel.setActiveTab(0) }, text = { Text("Dashboard") })
                            Tab(selected = state.activeTab == 1, onClick = { viewModel.setActiveTab(1) }, text = { Text("Matched (${state.matchedTotal})") })
                            Tab(selected = state.activeTab == 2, onClick = { viewModel.setActiveTab(2) }, text = { Text("Unmatched (${state.unmatchedTotal})") })
                            Tab(selected = state.activeTab == 3, onClick = { viewModel.setActiveTab(3) }, text = { Text("Pending (${state.pendingTotal})") })
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (state.activeTab) {
                            1 -> {
                                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    itemsIndexed(state.matchedBatches) { idx, b ->
                                        BatchListItem(batchNumber = b.rawValue, index = idx + 1, onDelete = {})
                                    }
                                }
                            }
                            2 -> {
                                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    itemsIndexed(state.unmatchedBatches) { idx, b ->
                                        BatchListItem(batchNumber = b.rawValue, index = idx + 1, onDelete = {})
                                    }
                                }
                            }
                            3 -> {
                                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    itemsIndexed(state.pendingBatches) { idx, b ->
                                        BatchListItem(batchNumber = b, index = idx + 1, onDelete = {})
                                    }
                                }
                            }
                            else -> {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Select a tab above to inspect batch details.\nDuplicate lines ignored: ${state.duplicateLinesIgnored}", fontSize = 16.sp)
                                }
                            }
                        }

                        // Export button
                        Button(
                            onClick = {
                                val textToShare = "Pending Batches:\n" + state.pendingBatches.joinToString("\n")
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Dispatch Verification Report")
                                    putExtra(Intent.EXTRA_TEXT, textToShare)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Verification Report"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            Text("Export Verification Report")
                        }
                    }
                }
            }
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
    }
}

@Composable
private fun CompactMetricCard(
    title: String,
    count: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 11.sp)
            Text(count.toString(), color = Color.White, fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
        }
    }
}
