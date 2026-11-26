package com.example.warehousescan.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

val Context.configDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_config_prefs")

interface ConfigProvider {
    val config: StateFlow<AppConfig>
    suspend fun applyOverride(json: String)
    suspend fun resetOverrides()
}

@Singleton
class ConfigProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ConfigProvider {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val _config = MutableStateFlow(AppConfig())
    override val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val overrideKey = stringPreferencesKey("runtime_config_override")

    init {
        scope.launch {
            loadInitialConfig()
        }
    }

    private suspend fun loadInitialConfig() {
        val defaultConfig = loadAssetConfig()
        var merged = defaultConfig

        try {
            val prefs = context.configDataStore.data.first()
            val overrideJson = prefs[overrideKey]
            if (!overrideJson.isNullOrBlank()) {
                val overrideConfig = jsonParser.decodeFromString<AppConfig>(overrideJson)
                validateConfig(overrideConfig)
                merged = overrideConfig
                Timber.d("Loaded runtime config override successfully.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load or validate runtime config override. Falling back to default.")
        }

        _config.value = merged
    }

    private fun loadAssetConfig(): AppConfig {
        return try {
            val inputStream = context.assets.open("config/app_config.json")
            val text = inputStream.bufferedReader().use { it.readText() }
            jsonParser.decodeFromString<AppConfig>(text)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load asset config, using hardcoded default.")
            AppConfig()
        }
    }

    override suspend fun applyOverride(json: String) {
        val newConfig = jsonParser.decodeFromString<AppConfig>(json)
        validateConfig(newConfig)

        // Persist to DataStore
        context.configDataStore.edit { prefs ->
            prefs[overrideKey] = json
        }
        _config.value = newConfig
        Timber.i("Successfully applied new config override.")
    }

    override suspend fun resetOverrides() {
        context.configDataStore.edit { prefs ->
            prefs.remove(overrideKey)
        }
        val defaultConfig = loadAssetConfig()
        _config.value = defaultConfig
        Timber.i("Reset config overrides to default asset config.")
    }

    fun validateConfig(cfg: AppConfig) {
        // 1. Validate Base URL
        val parsedUrl = cfg.api.baseUrl.toHttpUrlOrNull()
        require(parsedUrl != null) { "Invalid base URL: ${cfg.api.baseUrl}" }
        require(parsedUrl.scheme == "https" || parsedUrl.scheme == "http") {
            "Base URL must use http or https scheme: ${cfg.api.baseUrl}"
        }

        // 2. Validate Timeouts
        require(cfg.api.connectTimeoutSec > 0) { "connectTimeoutSec must be > 0" }
        require(cfg.api.readTimeoutSec > 0) { "readTimeoutSec must be > 0" }
        require(cfg.api.writeTimeoutSec > 0) { "writeTimeoutSec must be > 0" }

        // 3. Validate Regexes
        val valCfg = cfg.validation
        Regex(valCfg.rackRegex)
        Regex(valCfg.zoneRegex)
        Regex(valCfg.settingRegex)
        Regex(valCfg.batchRegex)
    }
}
