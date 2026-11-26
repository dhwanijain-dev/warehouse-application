package com.example.warehousescan.domain

import com.example.warehousescan.core.config.TxtFileConfig
import com.example.warehousescan.core.config.VerificationConfig
import com.example.warehousescan.domain.engine.*
import com.example.warehousescan.domain.model.Location
import com.example.warehousescan.domain.model.ScanStatus
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class DomainEngineTest {

    @Test
    fun testTemplateRendererAndSanitization() {
        val template = "Location - {rack}-{zone}-{setting} - {user} - {count}"
        val rendered = TemplateRenderer.render(
            template = template,
            rack = "111",
            zone = "01",
            setting = "03",
            user = "john.doe",
            count = 5,
            date = Date(0L)
        )
        assertTrue(rendered.contains("111-01-03"))
        assertTrue(rendered.contains("john.doe"))
        assertTrue(rendered.contains("5"))

        val dirtyName = "rack/zone:setting*.txt"
        val clean = TemplateRenderer.sanitizeFilename(dirtyName)
        assertEquals("rack_zone_setting_.txt", clean)
    }

    @Test
    fun testTxtFileGenerator() {
        val location = Location(rack = "111", zone = "01", setting = "03")
        val batches = listOf("100001", "100002", "100003")
        val config = TxtFileConfig(
            headerLine = "Batch Number",
            writeHeader = true,
            includeLocationLines = true,
            lineSeparator = "\n"
        )

        val content = TxtFileGenerator.generateContent(location, batches, config)
        val expected = "Rack: 111\nZone: 01\nSetting: 03\nBatch Number\n100001\n100002\n100003\n"
        assertEquals(expected, content)
    }

    @Test
    fun testTxtFileParser() {
        val fileContent = "Batch Number\r\n100001\r\n \r\n100002\r\n100001\r\n"
        val config = VerificationConfig(
            caseSensitive = false,
            trimWhitespace = true,
            skipHeaderLineValues = listOf("Batch Number"),
            duplicatesInUploadedFile = "ignore"
        )

        val result = TxtFileParser.parse(fileContent, config)
        assertEquals(2, result.batches.size)
        assertEquals("100001", result.batches[0])
        assertEquals("100002", result.batches[1])
        assertEquals(1, result.duplicateLinesIgnored)
    }

    @Test
    fun testVerificationEngineAndInvariants() {
        val uploaded = listOf("B001", "B002", "B003")
        val config = VerificationConfig(caseSensitive = false, trimWhitespace = true)
        val engine = VerificationEngine(uploaded, config)

        // Initial invariants
        engine.assertInvariants()
        assertEquals(3, engine.uploadedTotal)
        assertEquals(3, engine.pendingTotal)

        // 1. Scan valid match
        val res1 = engine.processScan("b001")
        assertEquals(ScanStatus.MATCHED, res1.status)
        assertEquals(1, engine.matchedTotal)
        assertEquals(2, engine.pendingTotal)
        engine.assertInvariants()

        // 2. Scan duplicate (already scanned)
        val res2 = engine.processScan("B001")
        assertEquals(ScanStatus.ALREADY_SCANNED, res2.status)
        assertEquals(1, engine.matchedTotal)
        engine.assertInvariants()

        // 3. Scan unmatched
        val res3 = engine.processScan("INVALID999")
        assertEquals(ScanStatus.UNMATCHED, res3.status)
        assertEquals(1, engine.unmatchedTotal)
        assertEquals(1, engine.matchedTotal)
        engine.assertInvariants()
    }
}
