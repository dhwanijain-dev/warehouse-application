package com.example.warehousescan.domain.model

data class Session(
    val user: User,
    val loginTimestamp: Long = System.currentTimeMillis()
)
