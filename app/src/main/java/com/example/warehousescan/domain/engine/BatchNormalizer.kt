package com.example.warehousescan.domain.engine

import com.example.warehousescan.domain.model.Batch

object BatchNormalizer {
    fun normalize(raw: String, caseSensitive: Boolean = false, trimWhitespace: Boolean = true): Batch {
        val trimmed = if (trimWhitespace) raw.trim() else raw
        val normalized = if (caseSensitive) trimmed else trimmed.uppercase()
        return Batch(
            rawValue = raw,
            normalizedValue = normalized
        )
    }
}
