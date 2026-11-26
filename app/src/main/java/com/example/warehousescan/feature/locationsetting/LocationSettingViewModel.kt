package com.example.warehousescan.feature.locationsetting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.core.storage.LocationChallanDao
import com.example.warehousescan.core.storage.LocationChallanEntity
import com.example.warehousescan.core.storage.LocationSessionDao
import com.example.warehousescan.core.storage.LocationSessionEntity
import com.example.warehousescan.domain.engine.BatchNormalizer
import com.example.warehousescan.domain.engine.ScanPayloadExtractor
import com.example.warehousescan.domain.model.Location
import com.example.warehousescan.domain.model.ScanEvent
import com.example.warehousescan.domain.model.User
import com.example.warehousescan.domain.repository.AuthRepository
import com.example.warehousescan.domain.repository.ScannerController
import com.example.warehousescan.domain.usecase.SubmitLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed interface LocationSettingUiState {
    data class Content(
        val rack: String = "",
        val zone: String = "",
        val setting: String = "",
        val locationValid: Boolean = false,
        val manualInput: String = "",
        val batches: List<String> = emptyList(),
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val isSubmitting: Boolean = false,
        val sessionSubmitted: Boolean = false,
        val showEmailConfirmation: Boolean = false
    ) : LocationSettingUiState
}

@HiltViewModel
class LocationSettingViewModel @Inject constructor(
    private val submitLocationUseCase: SubmitLocationUseCase,
    private val configProvider: ConfigProvider,
    private val scannerController: ScannerController,
    private val authRepository: AuthRepository,
    private val locationSessionDao: LocationSessionDao,
    private val locationChallanDao: LocationChallanDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationSettingUiState>(LocationSettingUiState.Content())
    val uiState: StateFlow<LocationSettingUiState> = _uiState.asStateFlow()
    val scannerStatus = scannerController.status

    val pendingChallans: StateFlow<List<LocationChallanEntity>> = locationChallanDao.getPendingChallans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentUser: User? = null
    private var screenResumed = false

    init {
        viewModelScope.launch {
            authRepository.fetchProfile().onSuccess { currentUser = it }
        }
        viewModelScope.launch {
            locationSessionDao.getLocationSession().collect { entity ->
                if (entity != null) {
                    val current = _uiState.value
                    if (current is LocationSettingUiState.Content && current.batches.isEmpty()) {
                        try {
                            val list = Json.decodeFromString<List<String>>(entity.batchesJson)
                            _uiState.value = current.copy(
                                rack = entity.rack,
                                zone = entity.zone,
                                setting = entity.setting,
                                locationValid = validLocation(entity.rack, entity.zone, entity.setting),
                                batches = list
                            )
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            scannerController.scanEvents.collect { event ->
                if (event is ScanEvent.Scanned) {
                    val extracted = ScanPayloadExtractor(configProvider.config.value.scanner.qrPayload).extract(event.barcode, event.symbology)
                    val regex = Regex(configProvider.config.value.validation.batchRegex)
                    if (extracted == null || !extracted.matches(regex)) {
                        scannerController.playFeedback("error")
                        updateContent { it.copy(errorMessage = "Invalid QR/Barcode format: ${event.barcode.take(80)}") }
                    } else {
                        val current = _uiState.value as? LocationSettingUiState.Content
                        scannerController.playFeedback(if (current?.batches?.contains(extracted) == true) "duplicate" else "success")
                        addBatch(extracted)
                    }
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

    override fun onCleared() {
        super.onCleared()
    }

    fun triggerScan() {
        scannerController.triggerSoftScan()
    }

    fun processCameraScan(rawText: String, symbology: String = "QRCODE") {
        scannerController.submitCameraScan(rawText, symbology)
    }

    fun updateRack(rack: String) {
        updateContent { it.copy(rack = rack, errorMessage = null) }
    }

    fun updateZone(zone: String) {
        updateContent { it.copy(zone = zone, errorMessage = null) }
    }

    fun updateSetting(setting: String) {
        updateContent { it.copy(setting = setting, errorMessage = null) }
    }

    fun updateManualInput(input: String) {
        updateContent { it.copy(manualInput = input, errorMessage = null) }
    }

    fun addBatch(rawCode: String) {
        val current = _uiState.value as? LocationSettingUiState.Content ?: return
        if (rawCode.isBlank()) return

        val valCfg = configProvider.config.value.validation
        val batchRegex = Regex(valCfg.batchRegex)
        val duplicatePolicy = configProvider.config.value.locationSetting.duplicateScanPolicy

        if (!rawCode.matches(batchRegex)) {
            _uiState.value = current.copy(errorMessage = "Invalid batch format: $rawCode")
            return
        }

        if (current.batches.contains(rawCode)) {
            when (duplicatePolicy) {
                "reject" -> {
                    _uiState.value = current.copy(errorMessage = "Batch already added: $rawCode")
                    return
                }
                "warn" -> {
                    // Add with warning message
                    val newList = current.batches + rawCode
                    _uiState.value = current.copy(batches = newList, manualInput = "", errorMessage = "Warning: Duplicate batch added: $rawCode")
                    saveSessionToDb(current.rack, current.zone, current.setting, newList)
                    return
                }
                "allow" -> {
                    // Add silently
                    val newList = current.batches + rawCode
                    _uiState.value = current.copy(batches = newList, manualInput = "", errorMessage = null)
                    saveSessionToDb(current.rack, current.zone, current.setting, newList)
                    return
                }
            }
        }

        val newList = current.batches + rawCode
        _uiState.value = current.copy(batches = newList, manualInput = "", errorMessage = null)
        saveSessionToDb(current.rack, current.zone, current.setting, newList)
    }

    fun removeBatch(index: Int) {
        val current = _uiState.value as? LocationSettingUiState.Content ?: return
        if (index in current.batches.indices) {
            val newList = current.batches.toMutableList().apply { removeAt(index) }
            _uiState.value = current.copy(batches = newList)
            saveSessionToDb(current.rack, current.zone, current.setting, newList)
        }
    }

    fun clearAll() {
        if (_uiState.value !is LocationSettingUiState.Content) return
        _uiState.value = LocationSettingUiState.Content()
        viewModelScope.launch {
            locationSessionDao.clearLocationSession()
        }
    }

    fun saveAndHoldChallan() {
        val current = _uiState.value as? LocationSettingUiState.Content ?: return
        if (current.rack.isBlank() && current.zone.isBlank() && current.setting.isBlank() && current.batches.isEmpty()) {
            _uiState.value = current.copy(errorMessage = "Cannot save empty challan session")
            return
        }
        viewModelScope.launch {
            val json = Json.encodeToString(current.batches)
            val existing = locationChallanDao.getChallanByLocation(current.rack, current.zone, current.setting)
            val challan = LocationChallanEntity(
                id = existing?.id ?: 0,
                rack = current.rack,
                zone = current.zone,
                setting = current.setting,
                batchesJson = json,
                status = "PENDING",
                updatedAt = System.currentTimeMillis()
            )
            locationChallanDao.saveChallan(challan)
            _uiState.value = LocationSettingUiState.Content(
                successMessage = "Challan saved for ${current.rack}-${current.zone}-${current.setting}. Started new location."
            )
            locationSessionDao.clearLocationSession()
        }
    }

    fun loadChallan(challan: LocationChallanEntity) {
        viewModelScope.launch {
            try {
                val list = Json.decodeFromString<List<String>>(challan.batchesJson)
                _uiState.value = LocationSettingUiState.Content(
                    rack = challan.rack,
                    zone = challan.zone,
                    setting = challan.setting,
                    locationValid = validLocation(challan.rack, challan.zone, challan.setting),
                    batches = list,
                    successMessage = "Reopened saved challan for ${challan.rack}-${challan.zone}-${challan.setting}"
                )
                saveSessionToDb(challan.rack, challan.zone, challan.setting, list)
            } catch (e: Exception) {
                _uiState.value = (_uiState.value as LocationSettingUiState.Content).copy(errorMessage = "Failed to load challan")
            }
        }
    }

    fun deleteChallan(id: Long) {
        viewModelScope.launch {
            locationChallanDao.deleteChallan(id)
        }
    }

    fun submit() {
        val current = _uiState.value as? LocationSettingUiState.Content ?: return
        val valCfg = configProvider.config.value.validation
        val location = Location(current.rack, current.zone, current.setting)

        if (!location.isValid(Regex(valCfg.rackRegex), Regex(valCfg.zoneRegex), Regex(valCfg.settingRegex))) {
            _uiState.value = current.copy(errorMessage = "Invalid location fields (Rack, Zone, Setting)")
            return
        }

        if (current.batches.isEmpty()) {
            _uiState.value = current.copy(errorMessage = "Cannot submit empty batch list")
            return
        }

        val user = currentUser ?: User("admin", "admin", "admin@example.com", "")
        if (user.email.isBlank()) {
            _uiState.value = current.copy(errorMessage = "User email is missing. Please update profile.")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true, errorMessage = null)
            val result = submitLocationUseCase(user, location, current.batches)
            if (result.isSuccess) {
                val emailProvider = configProvider.config.value.providers.email
                val showConfirm = (emailProvider == "intent")
                _uiState.value = current.copy(
                    isSubmitting = false,
                    successMessage = if (showConfirm) "Email app opened." else "Session successfully submitted & saved!",
                    sessionSubmitted = true,
                    showEmailConfirmation = showConfirm
                )
                if (!showConfirm) {
                    locationSessionDao.clearLocationSession()
                }
            } else {
                _uiState.value = current.copy(
                    isSubmitting = false,
                    errorMessage = "Failed to send email: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun confirmEmailSent(sent: Boolean) {
        val current = _uiState.value as? LocationSettingUiState.Content ?: return
        if (sent) {
            _uiState.value = current.copy(
                showEmailConfirmation = false,
                successMessage = "Session completed and cleared."
            )
            viewModelScope.launch {
                locationSessionDao.clearLocationSession()
            }
        } else {
            _uiState.value = current.copy(
                showEmailConfirmation = false,
                errorMessage = "Email not sent. You can retry submission."
            )
        }
    }

    private fun updateContent(block: (LocationSettingUiState.Content) -> LocationSettingUiState.Content) {
        val current = _uiState.value
        if (current is LocationSettingUiState.Content) {
            val transformed = block(current)
            val updated = transformed.copy(
                locationValid = validLocation(transformed.rack, transformed.zone, transformed.setting)
            )
            _uiState.value = updated
            syncScanner()
            saveSessionToDb(updated.rack, updated.zone, updated.setting, updated.batches)
        }
    }

    private fun validLocation(rack: String, zone: String, setting: String): Boolean {
        val cfg = configProvider.config.value.validation
        return rack.matches(Regex(cfg.rackRegex)) && zone.matches(Regex(cfg.zoneRegex)) && setting.matches(Regex(cfg.settingRegex))
    }

    private fun syncScanner() {
        if (screenResumed) scannerController.enableScanner()
        else scannerController.disableScanner()
    }

    private fun saveSessionToDb(rack: String, zone: String, setting: String, batches: List<String>) {
        viewModelScope.launch {
            val json = Json.encodeToString(batches)
            locationSessionDao.saveLocationSession(
                LocationSessionEntity(
                    rack = rack,
                    zone = zone,
                    setting = setting,
                    batchesJson = json
                )
            )
        }
    }
}
