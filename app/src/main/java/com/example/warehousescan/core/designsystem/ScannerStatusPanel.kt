package com.example.warehousescan.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.warehousescan.R
import com.example.warehousescan.domain.model.ScannerStatus
import com.example.warehousescan.ui.theme.WarehouseSuccess
import com.example.warehousescan.ui.theme.WarehouseWarning
import com.example.warehousescan.ui.theme.WarehouseError
import kotlinx.coroutines.delay

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
fun ScannerStatusPanel(status: ScannerStatus) {
    var showDetails by remember { mutableStateOf(false) }
    var showLastScan by remember(status.lastScanTimestamp) { mutableStateOf(status.lastScanValue != null) }
    var showFlash by remember { mutableStateOf(false) }
    val ready = status.dataWedgePresent && status.profileState == "Ready"
    val color = if (ready) WarehouseSuccess else WarehouseWarning

    LaunchedEffect(status.feedbackTimestamp) {
        if (status.feedbackResult != null) {
            showFlash = true
            delay(400)
            showFlash = false
        }
    }

    LaunchedEffect(status.lastScanTimestamp) {
        if (status.lastScanValue != null) {
            showLastScan = true
            delay(3_000)
            showLastScan = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        color = if (showFlash) when (status.feedbackResult) {
            "success" -> WarehouseSuccess.copy(alpha = 0.16f)
            "duplicate" -> WarehouseWarning.copy(alpha = 0.18f)
            else -> WarehouseError.copy(alpha = 0.14f)
        } else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showDetails = true }
                    .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier.padding(2.dp).background(color, CircleShape).padding(5.dp)
                )
                if (showFlash) {
                    val flashColor = when (status.feedbackResult) {
                        "success" -> WarehouseSuccess
                        "duplicate" -> WarehouseWarning
                        else -> WarehouseError
                    }
                    Icon(
                        imageVector = when (status.feedbackResult) {
                            "success" -> Icons.Default.CheckCircle
                            "duplicate" -> Icons.Default.Repeat
                            else -> Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = flashColor
                    )
                    Text(
                        text = stringResource(when (status.feedbackResult) {
                            "success" -> R.string.scan_accepted
                            "duplicate" -> R.string.scan_duplicate
                            else -> R.string.scan_invalid
                        }),
                        color = flashColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                Text(
                    text = stringResource(if (ready) R.string.scanner_ready else R.string.scanner_not_ready),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge
                )
                }
            }
            if (showLastScan && status.lastScanValue != null) {
                Text(
                    text = stringResource(R.string.last_scan_format, status.lastScanValue, symbologyLabel(status.lastSymbology)),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showDetails) {
        ModalBottomSheet(onDismissRequest = { showDetails = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.scanner_details_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.scanner_datawedge_status, stringResource(if (status.dataWedgePresent) R.string.present else R.string.missing)))
                Text(stringResource(R.string.scanner_version_format, status.dataWedgeVersion))
                Text(stringResource(R.string.scanner_profile_format, status.profileState))
                Text(stringResource(R.string.scanner_enabled_format, stringResource(if (status.scannerEnabled) R.string.enabled else R.string.disabled)))
                Text(stringResource(R.string.scanner_symbology_format, status.lastSymbology))
                status.warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun symbologyLabel(value: String): String = when (value.uppercase().removePrefix("LABEL-TYPE-")) {
    "QRCODE", "QR_CODE" -> stringResource(R.string.symbology_qr)
    "MICROQR", "MICRO_QR" -> stringResource(R.string.symbology_micro_qr)
    "CODE128", "CODE_128" -> stringResource(R.string.symbology_code128)
    "CODE39", "CODE_39" -> stringResource(R.string.symbology_code39)
    "DATAMATRIX", "DATA_MATRIX" -> stringResource(R.string.symbology_datamatrix)
    "EAN13", "EAN_13" -> stringResource(R.string.symbology_ean13)
    "EAN8", "EAN_8" -> stringResource(R.string.symbology_ean8)
    "UPCA", "UPC_A" -> stringResource(R.string.symbology_upca)
    else -> value
}
