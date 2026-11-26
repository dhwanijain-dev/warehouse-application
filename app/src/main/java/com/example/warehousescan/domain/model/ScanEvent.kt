package com.example.warehousescan.domain.model

sealed interface ScanEvent {
    data class Scanned(
        val barcode: String,
        val symbology: String = "UNKNOWN",
        val source: String = "scanner",
        val timestamp: Long = System.currentTimeMillis()
    ) : ScanEvent
    data class Error(val message: String) : ScanEvent
}

data class ScannerStatus(
    val dataWedgePresent: Boolean = false,
    val profileState: String = "Checking",
    val scannerEnabled: Boolean = false,
    val dataWedgeVersion: String = "Unknown",
    val lastSymbology: String = "—",
    val lastScanValue: String? = null,
    val lastScanTimestamp: Long = 0L,
    val feedbackResult: String? = null,
    val feedbackTimestamp: Long = 0L,
    val warning: String? = null
)
