package com.example.warehousescan.domain.engine

import com.example.warehousescan.core.config.QrPayloadConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Converts a single scanner payload into one candidate batch value. */
class ScanPayloadExtractor(private val config: QrPayloadConfig) {
    fun extract(rawText: String, symbology: String = "UNKNOWN"): String? {
        val normalizedSymbology = symbology.uppercase().removePrefix("LABEL-TYPE-").replace("-", "")
        val accepted = config.acceptedSymbologies.map { it.uppercase().replace("-", "") }
        if (config.rejectUnknownSymbology && normalizedSymbology !in accepted) return null
        val cleaned = if (config.stripControlChars) rawText.filterNot { it.code < 32 || it.code == 127 } else rawText
        val source = if (config.trim) cleaned.trim() else cleaned
        val value = when (config.mode.lowercase()) {
            "raw" -> source
            "regex" -> runCatching {
                val match = Regex(config.regex).find(source) ?: return null
                match.groups[config.regexGroup]?.value ?: match.groupValues.getOrNull(1)
            }.getOrNull()
            "delimiter" -> source.split(config.delimiter).getOrNull(config.delimiterIndex)
            "jsonfield" -> runCatching {
                Json.parseToJsonElement(source).jsonObject[config.jsonField]?.jsonPrimitive?.content
            }.getOrNull()
            else -> null
        } ?: return null
        val result = if (config.trim) value.trim() else value
        return result.takeIf { it.isNotEmpty() }
    }
}
