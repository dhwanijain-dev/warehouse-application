package com.example.warehousescan.domain.model

enum class ScanStatus {
    MATCHED,
    ALREADY_SCANNED,
    UNMATCHED
}

data class ScanResult(
    val batch: Batch,
    val status: ScanStatus,
    val message: String
)

data class VerificationDashboardState(
    val uploadedTotal: Int = 0,
    val uniqueScannedTotal: Int = 0,
    val scanAttemptsTotal: Int = 0,
    val matchedTotal: Int = 0,
    val unmatchedTotal: Int = 0,
    val pendingTotal: Int = 0,
    val duplicateLinesIgnored: Int = 0,
    val matchedBatches: List<Batch> = emptyList(),
    val unmatchedBatches: List<Batch> = emptyList(),
    val pendingBatches: List<String> = emptyList()
)
