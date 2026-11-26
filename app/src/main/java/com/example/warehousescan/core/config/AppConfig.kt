package com.example.warehousescan.core.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val schemaVersion: Int = 2,
    val environment: String = "prod",
    val api: ApiConfig = ApiConfig(),
    val providers: ProvidersConfig = ProvidersConfig(),
    val session: SessionConfig = SessionConfig(),
    val locationSetting: LocationSettingConfig = LocationSettingConfig(),
    val admin: AdminConfig = AdminConfig(),
    val smtp: SmtpConfig = SmtpConfig(),
    val scanner: ScannerConfig = ScannerConfig(),
    val txtFile: TxtFileConfig = TxtFileConfig(),
    val email: EmailConfig = EmailConfig(),
    val verification: VerificationConfig = VerificationConfig(),
    val validation: ValidationConfig = ValidationConfig(),
    val features: FeaturesConfig = FeaturesConfig()
)

@Serializable
data class ApiConfig(
    val baseUrl: String = "https://your-server.example.com/",
    val connectTimeoutSec: Long = 15,
    val readTimeoutSec: Long = 30,
    val writeTimeoutSec: Long = 30,
    val retryCount: Int = 2,
    val authHeaderName: String = "Authorization",
    val authHeaderPrefix: String = "Bearer ",
    val endpoints: EndpointsConfig = EndpointsConfig(),
    val fieldMap: Map<String, String> = mapOf(
        "login.request.username" to "username",
        "login.request.password" to "password",
        "login.response.userId" to "data.userId",
        "login.response.email" to "data.email",
        "login.response.token" to "data.token"
    )
)

@Serializable
data class EndpointsConfig(
    val login: EndpointItem = EndpointItem("POST", "api/v1/auth/login"),
    val sendTxtEmail: EndpointItem = EndpointItem("POST", "api/v1/location/email"),
    val profile: EndpointItem = EndpointItem("GET", "api/v1/users/me")
)

@Serializable
data class EndpointItem(
    val method: String = "GET",
    val path: String = ""
)

@Serializable
data class ProvidersConfig(
    val auth: String = "local",
    val email: String = "intent",
    val scanner: String = "datawedge"
)

@Serializable
data class SessionConfig(
    val timeoutMinutes: Int = 0
)

@Serializable
data class LocationSettingConfig(
    val duplicateScanPolicy: String = "reject" // reject, warn, allow
)

@Serializable
data class AdminConfig(
    val pin: String = "1234"
)

@Serializable
data class SmtpConfig(
    val host: String = "smtp.example.com",
    val port: Int = 587,
    val security: String = "starttls",
    val senderAddress: String = "noreply@example.com",
    val username: String = "",
    val password: String = ""
)

@Serializable
data class ScannerConfig(
    val dataWedgeIntentAction: String = "com.warehouse.scan.ACTION",
    val profileName: String = "WarehouseScanProfile",
    val debounceMs: Long = 500,
    val symbologies: List<String> = listOf("code128", "code39", "ean13", "ean8", "upca", "qrcode", "microqr", "datamatrix"),
    val feedbackSound: Boolean = true,
    val feedbackVibrate: Boolean = true,
    val qrPayload: QrPayloadConfig = QrPayloadConfig()
)

@Serializable
data class QrPayloadConfig(
    val mode: String = "raw",
    val stripControlChars: Boolean = true,
    val trim: Boolean = true,
    val regex: String = "^(?<batch>[A-Za-z0-9-]{3,30})$",
    val regexGroup: String = "batch",
    val delimiter: String = "|",
    val delimiterIndex: Int = 0,
    val jsonField: String = "batch",
    val acceptedSymbologies: List<String> = listOf("QRCODE", "MICROQR", "CODE128", "CODE39", "DATAMATRIX", "EAN13", "EAN8", "UPCA"),
    val rejectUnknownSymbology: Boolean = false
)

@Serializable
data class TxtFileConfig(
    val encoding: String = "UTF-8",
    val lineSeparator: String = "\n",
    val headerLine: String = "Batch Number",
    val writeHeader: Boolean = true,
    val fileNamePattern: String = "{rack}-{zone}-{setting}.txt",
    val includeLocationLines: Boolean = false
)

@Serializable
data class EmailConfig(
    val subjectTemplate: String = "Location Setting - {rack}-{zone}-{setting}",
    val bodyTemplate: String = "User: {user}\nDate: {dateTime}\nLocation: {rack}-{zone}-{setting}\nBatches: {count}"
)

@Serializable
data class VerificationConfig(
    val caseSensitive: Boolean = false,
    val trimWhitespace: Boolean = true,
    val skipHeaderLineValues: List<String> = listOf("Batch Number"),
    val totalScannedMeansUnique: Boolean = true,
    val duplicatesInUploadedFile: String = "ignore"
)

@Serializable
data class ValidationConfig(
    val rackRegex: String = "^[A-Za-z0-9]+$",
    val zoneRegex: String = "^[A-Za-z0-9]+$",
    val settingRegex: String = "^[A-Za-z0-9]+$",
    val batchRegex: String = "^[A-Za-z0-9-]{3,30}$"
)

@Serializable
data class FeaturesConfig(
    val manualEntry: Boolean = true,
    val exportVerificationResult: Boolean = true,
    val allowRuntimeConfigOverride: Boolean = true,
    val offlineLoginCache: Boolean = false
)
