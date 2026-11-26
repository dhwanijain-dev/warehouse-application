package com.example.warehousescan.domain.engine

import com.example.warehousescan.core.config.VerificationConfig

data class ParseResult(
    val batches: List<String>,
    val duplicateLinesIgnored: Int
)

object TxtFileParser {

    fun parse(fileContent: String, config: VerificationConfig): ParseResult {
        val lines = fileContent.lines()
        val seen = mutableSetOf<String>()
        val resultBatches = mutableListOf<String>()
        var duplicatesIgnored = 0

        for (rawLine in lines) {
            val line = if (config.trimWhitespace) rawLine.trim() else rawLine
            if (line.isBlank()) continue

            // Check skip headers
            if (config.skipHeaderLineValues.any { it.equals(line, ignoreCase = !config.caseSensitive) }) {
                continue
            }

            val normalized = if (config.caseSensitive) line else line.uppercase()

            if (seen.contains(normalized)) {
                duplicatesIgnored++
            } else {
                seen.add(normalized)
                resultBatches.add(line) // keep original text for display
            }
        }

        return ParseResult(
            batches = resultBatches,
            duplicateLinesIgnored = duplicatesIgnored
        )
    }
}
