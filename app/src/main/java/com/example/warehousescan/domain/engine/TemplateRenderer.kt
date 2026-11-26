package com.example.warehousescan.domain.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TemplateRenderer {

    fun render(
        template: String,
        rack: String,
        zone: String,
        setting: String,
        user: String,
        count: Int,
        date: Date = Date()
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateTimeStr = dateFormat.format(date)

        return template
            .replace("{rack}", rack)
            .replace("{zone}", zone)
            .replace("{setting}", setting)
            .replace("{user}", user)
            .replace("{dateTime}", dateTimeStr)
            .replace("{count}", count.toString())
    }

    fun sanitizeFilename(filename: String): String {
        // Remove characters illegal in file names: \ / : * ? " < > |
        return filename.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}
