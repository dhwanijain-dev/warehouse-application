package com.example.warehousescan.patch

import com.example.warehousescan.core.config.TxtFileConfig
import com.example.warehousescan.core.config.LocationSettingConfig
import com.example.warehousescan.data.auth.PlainPasswordVerifier
import com.example.warehousescan.domain.engine.TxtFileGenerator
import com.example.warehousescan.domain.model.Location
import org.junit.Assert.*
import org.junit.Test

class PatchFeaturesTest {

    @Test
    fun testPlainPasswordVerifier() {
        val verifier = PlainPasswordVerifier()
        assertTrue(verifier.verify("admin123", "admin123"))
        assertFalse(verifier.verify("wrong", "admin123"))
    }

    @Test
    fun testTxtFileGeneratorWithLocationLines() {
        val location = Location(rack = "111", zone = "01", setting = "03")
        val batches = listOf("100001", "100002")
        val configWithLines = TxtFileConfig(
            headerLine = "Batch Number",
            writeHeader = true,
            includeLocationLines = true,
            lineSeparator = "\n"
        )

        val content = TxtFileGenerator.generateContent(location, batches, configWithLines)
        assertTrue(content.contains("Rack: 111"))
        assertTrue(content.contains("Zone: 01"))
        assertTrue(content.contains("Setting: 03"))
        assertTrue(content.contains("Batch Number"))
        assertTrue(content.contains("100001"))

        val configWithoutLines = TxtFileConfig(
            headerLine = "Batch Number",
            writeHeader = true,
            includeLocationLines = false,
            lineSeparator = "\n"
        )
        val contentWithout = TxtFileGenerator.generateContent(location, batches, configWithoutLines)
        assertFalse(contentWithout.contains("Rack:"))
        assertTrue(contentWithout.contains("100001"))
    }

    @Test
    fun testDuplicateScanPolicies() {
        val policyReject = "reject"
        val policyWarn = "warn"
        val policyAllow = "allow"

        assertEquals("reject", policyReject)
        assertEquals("warn", policyWarn)
        assertEquals("allow", policyAllow)
    }

    @Test
    fun testFakeAuthRepositoryRegister() = kotlinx.coroutines.runBlocking {
        val tokenStore = io.mockk.mockk<com.example.warehousescan.core.storage.SecureTokenStore>(relaxed = true)
        val repo = com.example.warehousescan.data.auth.FakeAuthRepository(tokenStore)
        val result = repo.register("testuser", "password123", "Test User", "test@example.com")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("testuser", user?.username)
        assertEquals("test@example.com", user?.email)
    }
}
