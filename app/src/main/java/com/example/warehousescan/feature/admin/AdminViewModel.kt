package com.example.warehousescan.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescan.core.config.ConfigProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

sealed interface AdminUiState {
    data class Content(
        val baseUrl: String,
        val environment: String,
        val authProvider: String,
        val emailProvider: String,
        val timeoutMinutes: Int,
        val duplicatePolicy: String,
        val includeLocationLines: Boolean,
        val adminPin: String,
        val smtpHost: String,
        val smtpPort: Int,
        val testResult: String? = null,
        val isTesting: Boolean = false
    ) : AdminUiState
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val configProvider: ConfigProvider,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val cfg = configProvider.config.value
    private val _uiState = MutableStateFlow<AdminUiState>(
        AdminUiState.Content(
            baseUrl = cfg.api.baseUrl,
            environment = cfg.environment,
            authProvider = cfg.providers.auth,
            emailProvider = cfg.providers.email,
            timeoutMinutes = cfg.session.timeoutMinutes,
            duplicatePolicy = cfg.locationSetting.duplicateScanPolicy,
            includeLocationLines = cfg.txtFile.includeLocationLines,
            adminPin = cfg.admin.pin,
            smtpHost = cfg.smtp.host,
            smtpPort = cfg.smtp.port
        )
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun updateSettings(
        baseUrl: String,
        authProvider: String,
        emailProvider: String,
        timeoutMinutes: Int,
        duplicatePolicy: String,
        includeLocationLines: Boolean,
        adminPin: String,
        smtpHost: String,
        smtpPort: Int
    ) {
        val current = _uiState.value as? AdminUiState.Content ?: return
        _uiState.value = current.copy(
            baseUrl = baseUrl,
            authProvider = authProvider,
            emailProvider = emailProvider,
            timeoutMinutes = timeoutMinutes,
            duplicatePolicy = duplicatePolicy,
            includeLocationLines = includeLocationLines,
            adminPin = adminPin,
            smtpHost = smtpHost,
            smtpPort = smtpPort
        )
    }

    fun testConnection() {
        val current = _uiState.value as? AdminUiState.Content ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isTesting = true, testResult = null)
            try {
                val request = Request.Builder().url(current.baseUrl).build()
                val response = okHttpClient.newCall(request).execute()
                val success = response.isSuccessful || response.code in 200..499
                _uiState.value = current.copy(
                    isTesting = false,
                    testResult = if (success) "Connection Successful (Code: ${response.code})" else "Failed (Code: ${response.code})"
                )
            } catch (e: Exception) {
                _uiState.value = current.copy(
                    isTesting = false,
                    testResult = "Connection Failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun applySettings(onApplied: () -> Unit) {
        val current = _uiState.value as? AdminUiState.Content ?: return
        viewModelScope.launch {
            try {
                val currentCfg = configProvider.config.value
                val newCfg = currentCfg.copy(
                    api = currentCfg.api.copy(baseUrl = current.baseUrl),
                    providers = currentCfg.providers.copy(auth = current.authProvider, email = current.emailProvider),
                    session = currentCfg.session.copy(timeoutMinutes = current.timeoutMinutes),
                    locationSetting = currentCfg.locationSetting.copy(duplicateScanPolicy = current.duplicatePolicy),
                    txtFile = currentCfg.txtFile.copy(includeLocationLines = current.includeLocationLines),
                    admin = currentCfg.admin.copy(pin = current.adminPin),
                    smtp = currentCfg.smtp.copy(host = current.smtpHost, port = current.smtpPort)
                )
                val jsonStr = kotlinx.serialization.json.Json.encodeToString(com.example.warehousescan.core.config.AppConfig.serializer(), newCfg)
                configProvider.applyOverride(jsonStr)
                onApplied()
            } catch (e: Exception) {
                _uiState.value = current.copy(testResult = "Error applying config: ${e.localizedMessage}")
            }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            configProvider.resetOverrides()
            val cfg = configProvider.config.value
            _uiState.value = AdminUiState.Content(
                baseUrl = cfg.api.baseUrl,
                environment = cfg.environment,
                authProvider = cfg.providers.auth,
                emailProvider = cfg.providers.email,
                timeoutMinutes = cfg.session.timeoutMinutes,
                duplicatePolicy = cfg.locationSetting.duplicateScanPolicy,
                includeLocationLines = cfg.txtFile.includeLocationLines,
                adminPin = cfg.admin.pin,
                smtpHost = cfg.smtp.host,
                smtpPort = cfg.smtp.port,
                testResult = "Settings reset to default"
            )
        }
    }
}
