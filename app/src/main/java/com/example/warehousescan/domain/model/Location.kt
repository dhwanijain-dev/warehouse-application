package com.example.warehousescan.domain.model

data class Location(
    val rack: String,
    val zone: String,
    val setting: String
) {
    fun isValid(rackRegex: Regex, zoneRegex: Regex, settingRegex: Regex): Boolean {
        return rack.matches(rackRegex) && zone.matches(zoneRegex) && setting.matches(settingRegex)
    }
}
