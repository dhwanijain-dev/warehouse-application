package com.example.warehousescan.domain.model

data class User(
    val userId: String,
    val username: String,
    val email: String,
    val token: String,
    val role: String = "USER",
    val displayName: String = ""
)
