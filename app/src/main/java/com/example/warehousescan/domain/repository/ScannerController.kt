package com.example.warehousescan.domain.repository

import com.example.warehousescan.domain.model.ScanEvent
import com.example.warehousescan.domain.model.ScannerStatus
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ScannerController {
    val scanEvents: SharedFlow<ScanEvent>
    val status: StateFlow<ScannerStatus>
    fun prepareScanner()
    fun enableScanner()
    fun disableScanner()
    fun triggerSoftScan()
    fun submitCameraScan(rawText: String, symbology: String = "QRCODE")
    fun playFeedback(result: String)
}
