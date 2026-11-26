package com.example.warehousescan.data.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.domain.model.ScanEvent
import com.example.warehousescan.domain.model.ScannerStatus
import com.example.warehousescan.domain.repository.ScannerController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataWedgeScannerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configProvider: ConfigProvider
) : ScannerController {
    private val _scanEvents = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 64)
    override val scanEvents: SharedFlow<ScanEvent> = _scanEvents.asSharedFlow()
    private val _status = MutableStateFlow(ScannerStatus())
    override val status: StateFlow<ScannerStatus> = _status.asStateFlow()
    private val queue = Channel<ScanEvent>(Channel.UNLIMITED)
    private var isRegistered = false
    private var enabled = false
    private var desiredEnabled = false
    private var lastScan = ""
    private var lastScanTime = 0L
    private var lastSymbology = "—"
    private val debounceLock = Any()
    private var setConfigSent = false
    @Volatile private var setupStarted = false
    @Volatile private var statusMessage = "DataWedge status pending"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val cfg = configProvider.config.value.scanner
            if (intent?.action != cfg.dataWedgeIntentAction) return
            val text = intent.getStringExtra("com.symbol.datawedge.data_string") ?: return
            val now = System.currentTimeMillis()
            val symbology = intent.getStringExtra("com.symbol.datawedge.label_type") ?: "UNKNOWN"
            val source = intent.getStringExtra("com.symbol.datawedge.source") ?: "scanner"
            publishScan(text, symbology, source, now)
        }
    }

    init {
        val resultFilter = IntentFilter("com.symbol.datawedge.api.RESULT_ACTION")
        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val result = intent?.getStringExtra("RESULT") ?: "UNKNOWN"
                val command = intent?.getStringExtra("COMMAND") ?: "DataWedge"
                val commandId = intent?.getStringExtra("COMMAND_IDENTIFIER")
                val info = intent?.getBundleExtra("RESULT_INFO")
                val version = info?.getString("DATAWEDGE") ?: info?.getString("datawedge")
                if (version != null) _status.value = _status.value.copy(dataWedgeVersion = version)
                if (commandId == CREATE_PROFILE_COMMAND) {
                    val resultCode = info?.getString("RESULT_CODE")
                    val alreadyExists = resultCode == "PROFILE_ALREADY_EXISTS"
                    Timber.i("DataWedge profile create result=$result resultCode=$resultCode")
                    if (result.equals("SUCCESS", true) || alreadyExists) {
                        sendProfileConfig()
                    } else {
                        val warning = "DataWedge profile creation failed: ${resultCode ?: result}"
                        _status.value = _status.value.copy(profileState = "Failed", warning = warning)
                    }
                } else if (commandId == SET_CONFIG_COMMAND) {
                    val results = intent?.getParcelableArrayListExtra<Bundle>("RESULT_LIST").orEmpty()
                    val failures = results.filter {
                        it.getString("RESULT")?.equals("FAILURE", true) == true || it.getString("RESULT_CODE") != null
                    }
                    val failed = result.equals("FAILURE", true) || failures.isNotEmpty()
                    results.forEach { Timber.i("DataWedge SET_CONFIG module result=$it") }
                    val warning = if (failed) "DataWedge SET_CONFIG failed: ${failures.joinToString()}" else null
                    _status.value = _status.value.copy(profileState = if (failed) "Failed" else "Ready", warning = warning)
                    Timber.i("DataWedge SET_CONFIG result=$result warning=$warning")
                    if (!failed && desiredEnabled && !enabled) scannerPlugin("ENABLE_PLUGIN")
                } else if (commandId == ENABLE_SCANNER_COMMAND || commandId == DISABLE_SCANNER_COMMAND) {
                    val failed = result.equals("FAILURE", true)
                    val shouldEnable = commandId == ENABLE_SCANNER_COMMAND
                    val warning = if (failed) "DataWedge scanner ${if (shouldEnable) "enable" else "disable"} failed" else null
                    _status.value = _status.value.copy(
                        scannerEnabled = !failed && shouldEnable,
                        warning = warning ?: _status.value.warning
                    )
                    Timber.i("DataWedge scanner plugin result=$result warning=$warning")
                }
                Timber.i("DataWedge result command=$command id=$commandId result=$result info=$info")
            }
        }, resultFilter, ContextCompat.RECEIVER_EXPORTED)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            for (event in queue) _scanEvents.emit(event)
        }
    }

    private fun setupDataWedgeProfile() {
        val cfg = configProvider.config.value.scanner
        val action = Intent("com.symbol.datawedge.api.ACTION").setPackage("com.symbol.datawedge")
        _status.value = _status.value.copy(profileState = "Creating profile")
        runCatching {
            context.sendBroadcast(Intent(action)
                .putExtra("com.symbol.datawedge.api.CREATE_PROFILE", cfg.profileName)
                .putExtra("SEND_RESULT", "LAST_RESULT")
                .putExtra("COMMAND_IDENTIFIER", CREATE_PROFILE_COMMAND))
        }.onFailure {
            statusMessage = "DataWedge profile creation failed: ${it.message}"
            _status.value = _status.value.copy(profileState = "Failed", warning = statusMessage)
            Timber.e(it, statusMessage)
        }
        queryVersion()
    }

    private fun sendProfileConfig() {
        if (setConfigSent) return
        setConfigSent = true
        val cfg = configProvider.config.value.scanner
        val action = Intent("com.symbol.datawedge.api.ACTION").setPackage("com.symbol.datawedge")
        val bar = Bundle().apply {
            putString("PLUGIN_NAME", "BARCODE"); putString("RESET_CONFIG", "true")
            putBundle("PARAM_LIST", Bundle().apply {
                putString("scanner_selection", "auto"); putString("scanner_input_enabled", "true")
                val syms = cfg.symbologies.map(String::lowercase).toSet()
                mapOf("qrcode" to true, "microqr" to ("microqr" in syms), "datamatrix" to ("datamatrix" in syms),
                    "code128" to ("code128" in syms), "code39" to ("code39" in syms), "ean13" to ("ean13" in syms),
                    "ean8" to ("ean8" in syms), "upca" to ("upca" in syms), "aztec" to false).forEach { (key, value) ->
                    putString("decoder_$key", value.toString())
                }
                putString("illumination_mode", "on"); putString("decode_audio_feedback", "false"); putString("decode_haptic_feedback", "false")
            })
        }
        val intentOutput = plugin("INTENT", Bundle().apply {
            putString("intent_output_enabled", "true"); putString("intent_action", cfg.dataWedgeIntentAction); putString("intent_delivery", "2")
        })
        val keystroke = plugin("KEYSTROKE", Bundle().apply { putString("keystroke_output_enabled", "false") })
        val app = Bundle().apply { putString("PACKAGE_NAME", context.packageName); putStringArray("ACTIVITY_LIST", arrayOf("*")) }
        val config = Bundle().apply {
            putString("PROFILE_NAME", cfg.profileName); putString("PROFILE_ENABLED", "true")
            putParcelableArrayList("PLUGIN_CONFIG", arrayListOf(bar, intentOutput, keystroke))
            putParcelableArray("APP_LIST", arrayOf(app))
        }
        runCatching {
            context.sendBroadcast(Intent(action)
                .putExtra("com.symbol.datawedge.api.SET_CONFIG", config)
                .putExtra("SEND_RESULT", "COMPLETE_RESULT")
                .putExtra("COMMAND_IDENTIFIER", SET_CONFIG_COMMAND))
        }
            .onSuccess { Timber.i("DataWedge SET_CONFIG broadcast sent") }
            .onFailure { statusMessage = "DataWedge SET_CONFIG failed: ${it.message}"; _status.value = _status.value.copy(profileState = "Failed", warning = statusMessage); Timber.e(it, statusMessage) }
    }

    private fun plugin(name: String, params: Bundle) = Bundle().apply {
        putString("PLUGIN_NAME", name); putString("RESET_CONFIG", "true"); putBundle("PARAM_LIST", params)
    }

    private fun queryVersion() {
        val version = runCatching {
            val info = if (Build.VERSION.SDK_INT >= 33) context.packageManager.getPackageInfo("com.symbol.datawedge", PackageManager.PackageInfoFlags.of(0))
            else @Suppress("DEPRECATION") context.packageManager.getPackageInfo("com.symbol.datawedge", 0)
            "${info.versionName ?: "installed"}"
        }.getOrNull()
        statusMessage = if (version == null) "DataWedge missing; camera or manual scanning required" else "DataWedge $version; profile setup sent"
        _status.value = _status.value.copy(
            dataWedgePresent = version != null,
            profileState = if (version == null) "Missing" else "Configuring",
            dataWedgeVersion = version ?: "Missing",
            warning = if (version == null) statusMessage else null
        )
        context.sendBroadcast(Intent("com.symbol.datawedge.api.ACTION").setPackage("com.symbol.datawedge")
            .putExtra("com.symbol.datawedge.api.GET_VERSION_INFO", "WarehouseScan")
            .putExtra("SEND_RESULT", "LAST_RESULT")
            .putExtra("COMMAND_IDENTIFIER", VERSION_COMMAND))
        Timber.i(statusMessage)
    }

    override fun enableScanner() {
        desiredEnabled = true
        if (!isRegistered) {
            val filter = IntentFilter(configProvider.config.value.scanner.dataWedgeIntentAction).apply { addCategory(Intent.CATEGORY_DEFAULT) }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            isRegistered = true
        }
        prepareScanner()
        if (!enabled && _status.value.profileState == "Ready") scannerPlugin("ENABLE_PLUGIN")
    }

    override fun prepareScanner() {
        if (!setupStarted) {
            synchronized(this) {
                if (!setupStarted) {
                    setupStarted = true
                    setupDataWedgeProfile()
                }
            }
        }
    }

    override fun disableScanner() {
        desiredEnabled = false
        if (enabled) scannerPlugin("DISABLE_PLUGIN")
        if (isRegistered) runCatching { context.unregisterReceiver(receiver) }.onFailure(Timber::e)
        isRegistered = false
    }

    private fun scannerPlugin(command: String) {
        val enabling = command == "ENABLE_PLUGIN"
        context.sendBroadcast(Intent("com.symbol.datawedge.api.ACTION").setPackage("com.symbol.datawedge")
            .putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", command)
            .putExtra("SEND_RESULT", "LAST_RESULT")
            .putExtra("COMMAND_IDENTIFIER", if (enabling) ENABLE_SCANNER_COMMAND else DISABLE_SCANNER_COMMAND))
        enabled = enabling
        _status.value = _status.value.copy(scannerEnabled = enabled)
        Timber.i("Scanner ${if (enabled) "enabled" else "disabled"}; $statusMessage; last symbology $lastSymbology")
    }

    override fun triggerSoftScan() {
        context.sendBroadcast(Intent("com.symbol.datawedge.api.ACTION").setPackage("com.symbol.datawedge")
            .putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", "START_SCANNING"))
    }

    override fun submitCameraScan(rawText: String, symbology: String) {
        publishScan(rawText, symbology, "camera", System.currentTimeMillis())
    }

    private fun publishScan(rawText: String, symbology: String, source: String, timestamp: Long) {
        val debounceMs = configProvider.config.value.scanner.debounceMs
        val accepted = synchronized(debounceLock) {
            if (rawText == lastScan && timestamp - lastScanTime < debounceMs) false
            else {
                lastScan = rawText
                lastScanTime = timestamp
                lastSymbology = symbology
                true
            }
        }
        if (!accepted) return
        _status.value = _status.value.copy(
            lastSymbology = symbology,
            lastScanValue = rawText,
            lastScanTimestamp = timestamp
        )
        queue.trySendBlocking(ScanEvent.Scanned(rawText, symbology, source, timestamp))
    }

    override fun playFeedback(result: String) {
        val cfg = configProvider.config.value.scanner
        _status.value = _status.value.copy(feedbackResult = result, feedbackTimestamp = System.currentTimeMillis())
        val duration = when (result) { "error" -> 420; "duplicate" -> 55; else -> 90 }
        val tone = when (result) {
            "error" -> ToneGenerator.TONE_CDMA_LOW_L
            "duplicate" -> ToneGenerator.TONE_PROP_NACK
            else -> ToneGenerator.TONE_PROP_ACK
        }
        if (cfg.feedbackSound) runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).startTone(tone, duration) }
        if (cfg.feedbackVibrate) runCatching {
            val pattern = when (result) { "error" -> longArrayOf(0, 350); "duplicate" -> longArrayOf(0, 45, 55, 45); else -> longArrayOf(0, 70) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(pattern, -1)
            }
        }.onFailure { Timber.e(it, "Scan vibration failed") }
    }

    private companion object {
        const val CREATE_PROFILE_COMMAND = "WAREHOUSE_SCAN_CREATE_PROFILE"
        const val SET_CONFIG_COMMAND = "WAREHOUSE_SCAN_SET_CONFIG"
        const val VERSION_COMMAND = "WAREHOUSE_SCAN_VERSION"
        const val ENABLE_SCANNER_COMMAND = "WAREHOUSE_SCAN_ENABLE_SCANNER"
        const val DISABLE_SCANNER_COMMAND = "WAREHOUSE_SCAN_DISABLE_SCANNER"
    }

}

@Singleton
class FakeScannerController @Inject constructor() : ScannerController {
    private val _scanEvents = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 64)
    override val scanEvents: SharedFlow<ScanEvent> = _scanEvents.asSharedFlow()
    private val _status = MutableStateFlow(ScannerStatus(true, "Ready (fake)", false, "Fake"))
    override val status: StateFlow<ScannerStatus> = _status.asStateFlow()
    private var testCounter = 100001
    override fun enableScanner() = Unit
    override fun prepareScanner() = Unit
    override fun disableScanner() = Unit
    override fun triggerSoftScan() { val value = testCounter++; _scanEvents.tryEmit(ScanEvent.Scanned(value.toString(), "QRCODE", "fake")) }
    override fun submitCameraScan(rawText: String, symbology: String) { _scanEvents.tryEmit(ScanEvent.Scanned(rawText, symbology, "camera")) }
    override fun playFeedback(result: String) {
        _status.value = _status.value.copy(feedbackResult = result, feedbackTimestamp = System.currentTimeMillis())
    }
    suspend fun simulateScan(barcode: String) = _scanEvents.emit(ScanEvent.Scanned(barcode, "QRCODE", "fake"))
}
