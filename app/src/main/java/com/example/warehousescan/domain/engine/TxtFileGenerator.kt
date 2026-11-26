package com.example.warehousescan.domain.engine

import com.example.warehousescan.core.config.TxtFileConfig
import com.example.warehousescan.domain.model.Location

object TxtFileGenerator {

    fun generateContent(
        location: Location,
        batches: List<String>,
        config: TxtFileConfig
    ): String {
        val sb = StringBuilder()
        val sep = config.lineSeparator

        if (config.includeLocationLines) {
            sb.append("Rack: ${location.rack}$sep")
            sb.append("Zone: ${location.zone}$sep")
            sb.append("Setting: ${location.setting}$sep")
        }

        if (config.writeHeader && config.headerLine.isNotBlank()) {
            sb.append("${config.headerLine}$sep")
        }

        for (batch in batches) {
            sb.append("$batch$sep")
        }

        return sb.toString()
    }
}
