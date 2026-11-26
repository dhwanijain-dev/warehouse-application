package com.example.warehousescan.core.config

import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ConfigProviderTest {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val configProvider = ConfigProviderImpl(mockk(relaxed = true))

    @Test
    fun testDefaultConfigValidatesSuccessfully() {
        val defaultConfig = AppConfig()
        // Should not throw
        configProvider.validateConfig(defaultConfig)
    }

    @Test
    fun testUnknownKeysIgnoredDuringParsing() {
        val jsonWithUnknown = """
            {
              "schemaVersion": 1,
              "environment": "prod",
              "unknownTopLevelField": "shouldBeIgnored",
              "api": {
                "baseUrl": "https://api.example.com/",
                "unknownApiField": 123
              }
            }
        """.trimIndent()

        val parsed = jsonParser.decodeFromString<AppConfig>(jsonWithUnknown)
        assertEquals("https://api.example.com/", parsed.api.baseUrl)
        assertEquals(1, parsed.schemaVersion)
        // Should validate successfully
        configProvider.validateConfig(parsed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidBaseUrlRejected() {
        val invalidConfig = AppConfig(
            api = ApiConfig(baseUrl = "ftp://invalid-url.com/")
        )
        configProvider.validateConfig(invalidConfig)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidTimeoutRejected() {
        val invalidConfig = AppConfig(
            api = ApiConfig(connectTimeoutSec = 0)
        )
        configProvider.validateConfig(invalidConfig)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidRegexRejected() {
        val invalidConfig = AppConfig(
            validation = ValidationConfig(rackRegex = "[unclosed-bracket")
        )
        configProvider.validateConfig(invalidConfig)
    }
}
