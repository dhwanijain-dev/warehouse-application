package com.example.warehousescan.domain.engine

import com.example.warehousescan.core.config.VerificationConfig
import com.example.warehousescan.domain.model.Batch
import com.example.warehousescan.domain.model.ScanResult
import com.example.warehousescan.domain.model.ScanStatus

class VerificationEngine(
    uploadedBatches: List<String>,
    private val config: VerificationConfig
) {
    private val uploadedSet = mutableSetOf<String>()
    private val matchedSet = mutableSetOf<String>()
    private val unmatchedSet = mutableSetOf<String>()
    private val scannedUniqueSet = mutableSetOf<String>()

    private val matchedList = mutableListOf<Batch>()
    private val unmatchedList = mutableListOf<Batch>()

    private var scanAttemptsTotal = 0

    init {
        for (b in uploadedBatches) {
            val norm = if (config.caseSensitive) b else b.uppercase()
            uploadedSet.add(norm)
        }
    }

    val uploadedTotal: Int get() = uploadedSet.size
    val scanAttempts: Int get() = scanAttemptsTotal
    val uniqueScannedTotal: Int get() = scannedUniqueSet.size
    val matchedTotal: Int get() = matchedSet.size
    val unmatchedTotal: Int get() = unmatchedSet.size
    val pendingTotal: Int get() = uploadedSet.size - matchedSet.size

    fun processScan(rawScan: String): ScanResult {
        scanAttemptsTotal++
        val batch = BatchNormalizer.normalize(rawScan, config.caseSensitive, config.trimWhitespace)
        val norm = batch.normalizedValue

        val isUploaded = uploadedSet.contains(norm)

        if (!isUploaded) {
            unmatchedSet.add(norm)
            scannedUniqueSet.add(norm)
            if (!unmatchedList.any { it.normalizedValue == norm }) {
                unmatchedList.add(batch)
            }
            return ScanResult(
                batch = batch,
                status = ScanStatus.UNMATCHED,
                message = "No Matching Found"
            )
        }

        // It is uploaded
        val isAlreadyMatched = matchedSet.contains(norm)
        scannedUniqueSet.add(norm)

        if (isAlreadyMatched) {
            return ScanResult(
                batch = batch,
                status = ScanStatus.ALREADY_SCANNED,
                message = "Already scanned"
            )
        } else {
            matchedSet.add(norm)
            if (!matchedList.any { it.normalizedValue == norm }) {
                matchedList.add(batch)
            }
            return ScanResult(
                batch = batch,
                status = ScanStatus.MATCHED,
                message = "Matched"
            )
        }
    }

    fun getMatchedBatches(): List<Batch> = matchedList.toList()
    fun getUnmatchedBatches(): List<Batch> = unmatchedList.toList()
    fun getPendingBatches(): List<String> {
        return uploadedSet.filter { !matchedSet.contains(it) }
    }

    fun assertInvariants() {
        val uploaded = uploadedTotal
        val matched = matchedTotal
        val pending = pendingTotal
        val uniqueScanned = uniqueScannedTotal
        val unmatched = unmatchedTotal

        require(matched + pending == uploaded) { "Invariant violated: Matched ($matched) + Pending ($pending) != Uploaded ($uploaded)" }
        require(matched <= uploaded) { "Invariant violated: Matched ($matched) > Uploaded ($uploaded)" }
        require(matched + unmatched == uniqueScanned) { "Invariant violated: Matched ($matched) + Unmatched ($unmatched) != UniqueScanned ($uniqueScanned)" }
    }
}
