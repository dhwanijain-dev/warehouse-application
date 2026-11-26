package com.example.warehousescan.domain

import com.example.warehousescan.core.config.QrPayloadConfig
import com.example.warehousescan.domain.engine.ScanPayloadExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanPayloadExtractorTest {
    @Test fun rawCleansControlsAndTrims() {
        val extractor = ScanPayloadExtractor(QrPayloadConfig())
        assertEquals("100001", extractor.extract("100001\r\n", "LABEL-TYPE-QRCODE"))
        assertEquals("100001", extractor.extract("100001", "QRCODE"))
        assertNull(extractor.extract("", "QRCODE"))
        assertEquals("garbage", extractor.extract("garbage", "UNKNOWN"))
    }

    @Test fun regexCanExtractBatchFromUrl() {
        val extractor = ScanPayloadExtractor(QrPayloadConfig(mode = "regex", regex = "/b/(?<batch>[A-Za-z0-9-]+)", regexGroup = "batch"))
        assertEquals("100001", extractor.extract("https://example.com/b/100001", "QRCODE"))
        assertNull(extractor.extract("garbage", "QRCODE"))
    }

    @Test fun delimiterAndJsonFieldModesExtractOneValue() {
        assertEquals("100001", ScanPayloadExtractor(QrPayloadConfig(mode = "delimiter")).extract("100001|ROLL7|2026", "CODE128"))
        assertEquals("100001", ScanPayloadExtractor(QrPayloadConfig(mode = "jsonField")).extract("{\"batch\":\"100001\"}", "QRCODE"))
        assertNull(ScanPayloadExtractor(QrPayloadConfig(mode = "jsonField")).extract("garbage", "QRCODE"))
    }
}
