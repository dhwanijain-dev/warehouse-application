package com.example.warehousescan.feature.dispatchverification

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.core.storage.VerificationSessionDao
import com.example.warehousescan.core.storage.VerificationSessionEntity
import com.example.warehousescan.domain.engine.VerificationEngine
import com.example.warehousescan.domain.engine.ScanPayloadExtractor
import com.example.warehousescan.domain.engine.TxtFileParser
import com.example.warehousescan.domain.model.Batch
import com.example.warehousescan.domain.model.ScanEvent
import com.example.warehousescan.domain.repository.ScannerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

sealed interface DispatchUiState {
    data class Content(
        val isUploaded: Boolean = false,
        val uploadedTotal: Int = 0,
        val scanAttemptsTotal: Int = 0,
        val uniqueScannedTotal: Int = 0,
        val matchedTotal: Int = 0,
        val unmatchedTotal: Int = 0,
        val pendingTotal: Int = 0,
        val duplicateLinesIgnored: Int = 0,
        val manualInput: String = "",
        val activeTab: Int = 0, // 0 = Dashboard, 1 = Matched, 2 = Unmatched, 3 = Pending
        val matchedBatches: List<Batch> = emptyList(),
        val unmatchedBatches: List<Batch> = emptyList(),
        val pendingBatches: List<String> = emptyList(),
        val lastScanMessage: String? = null,
        val lastScanStatus: String? = null,
        val errorMessage: String? = null
    ) : DispatchUiState
}

@HiltViewModel
class DispatchVerificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configProvider: ConfigProvider,
    private val scannerController: ScannerController,
    private val verificationSessionDao: VerificationSessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<DispatchUiState>(DispatchUiState.Content())
    val uiState: StateFlow<DispatchUiState> = _uiState.asStateFlow()
    val scannerStatus = scannerController.status

    private var verificationEngine: VerificationEngine? = null
    private var screenResumed = false

    init {
        viewModelScope.launch {
            scannerController.scanEvents.collect { event ->
                if (event is ScanEvent.Scanned) {
                    val extracted = ScanPayloadExtractor(configProvider.config.value.scanner.qrPayload).extract(event.barcode, event.symbology)
                    if (extracted == null || !extracted.matches(Regex(configProvider.config.value.validation.batchRegex))) {
                        scannerController.playFeedback("error")
                        val current = _uiState.value as? DispatchUiState.Content
                        if (current != null) _uiState.value = current.copy(errorMessage = "Invalid QR/Barcode format: ${event.barcode.take(80)}")
                    } else processScan(extracted, fromScanner = true)
                }
            }
        }
    }

    fun onScreenResumed() {
        screenResumed = true
        scannerController.prepareScanner()
        syncScanner()
    }

    fun onScreenPaused() {
        screenResumed = false
        scannerController.disableScanner()
    }

    private fun syncScanner() {
        val uploaded = (_uiState.value as? DispatchUiState.Content)?.isUploaded == true
        if (screenResumed && uploaded) scannerController.enableScanner() else scannerController.disableScanner()
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun triggerScan() {
        scannerController.triggerSoftScan()
    }

    fun processCameraScan(rawText: String, symbology: String = "QRCODE") {
        scannerController.submitCameraScan(rawText, symbology)
    }

    fun parseFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val content = reader.readText()
                reader.close()

                val cfg = configProvider.config.value.verification
                val parseResult = TxtFileParser.parse(content, cfg)

                val engine = VerificationEngine(parseResult.batches, cfg)
                verificationEngine = engine

                updateStateFromEngine(
                    duplicateIgnored = parseResult.duplicateLinesIgnored,
                    error = null
                )
            } catch (e: Exception) {
                val current = _uiState.value as? DispatchUiState.Content ?: return@launch
                _uiState.value = current.copy(errorMessage = "Failed to parse file: ${e.localizedMessage}")
            }
        }
    }

    fun updateManualInput(input: String) {
        val current = _uiState.value as? DispatchUiState.Content ?: return
        _uiState.value = current.copy(manualInput = input)
    }

    fun processScan(barcode: String, fromScanner: Boolean = false) {
        val engine = verificationEngine
        val current = _uiState.value as? DispatchUiState.Content ?: return

        if (engine == null) {
            if (fromScanner) scannerController.playFeedback("error")
            _uiState.value = current.copy(errorMessage = "Please upload a dispatch list first!")
            return
        }

        val res = engine.processScan(barcode)
        if (fromScanner) {
            val feedback = when (res.status) {
                com.example.warehousescan.domain.model.ScanStatus.ALREADY_SCANNED -> "duplicate"
                com.example.warehousescan.domain.model.ScanStatus.UNMATCHED -> "error"
                com.example.warehousescan.domain.model.ScanStatus.MATCHED -> "success"
            }
            scannerController.playFeedback(feedback)
        }
        engine.assertInvariants()

        _uiState.value = current.copy(
            manualInput = "",
            uploadedTotal = engine.uploadedTotal,
            scanAttemptsTotal = engine.scanAttempts,
            uniqueScannedTotal = engine.uniqueScannedTotal,
            matchedTotal = engine.matchedTotal,
            unmatchedTotal = engine.unmatchedTotal,
            pendingTotal = engine.pendingTotal,
            matchedBatches = engine.getMatchedBatches(),
            unmatchedBatches = engine.getUnmatchedBatches(),
            pendingBatches = engine.getPendingBatches(),
            lastScanMessage = res.message,
            lastScanStatus = res.status.name,
            errorMessage = null
        )
    }

    fun setActiveTab(tab: Int) {
        val current = _uiState.value as? DispatchUiState.Content ?: return
        _uiState.value = current.copy(activeTab = tab)
    }

    private fun updateStateFromEngine(duplicateIgnored: Int, error: String?) {
        val engine = verificationEngine ?: return
        val current = _uiState.value as? DispatchUiState.Content ?: return

        _uiState.value = current.copy(
            isUploaded = true,
            uploadedTotal = engine.uploadedTotal,
            scanAttemptsTotal = engine.scanAttempts,
            uniqueScannedTotal = engine.uniqueScannedTotal,
            matchedTotal = engine.matchedTotal,
            unmatchedTotal = engine.unmatchedTotal,
            pendingTotal = engine.pendingTotal,
            duplicateLinesIgnored = duplicateIgnored,
            matchedBatches = engine.getMatchedBatches(),
            unmatchedBatches = engine.getUnmatchedBatches(),
            pendingBatches = engine.getPendingBatches(),
            errorMessage = error
        )
        syncScanner()
    }
}
