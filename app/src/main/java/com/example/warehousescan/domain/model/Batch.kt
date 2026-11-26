package com.example.warehousescan.domain.model

data class Batch(
    val rawValue: String,
    val normalizedValue: String,
    val timestamp: Long = System.currentTimeMillis()
)
