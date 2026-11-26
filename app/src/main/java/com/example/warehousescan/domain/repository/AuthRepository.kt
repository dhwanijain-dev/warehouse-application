package com.example.warehousescan.domain.repository

import com.example.warehousescan.domain.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun register(username: String, password: String, displayName: String, email: String): Result<User>
    suspend fun fetchProfile(): Result<User>
}
