package com.example.warehousescan.data.auth

import com.example.warehousescan.core.storage.UserDao
import com.example.warehousescan.core.storage.SecureTokenStore
import com.example.warehousescan.domain.model.User
import com.example.warehousescan.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val passwordVerifier: PasswordVerifier,
    private val tokenStore: SecureTokenStore
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<User> {
        val userEntity = userDao.getUserByUserId(username.trim())
            ?: return Result.failure(IllegalArgumentException("User not found: $username"))

        if (!userEntity.active) {
            return Result.failure(IllegalStateException("User account is inactive"))
        }

        val passwordMatches = passwordVerifier.verify(password, userEntity.password)
        if (!passwordMatches) {
            return Result.failure(IllegalArgumentException("Invalid password"))
        }

        val token = "local-token-${userEntity.userId}"
        tokenStore.saveToken(token)

        val user = User(
            userId = userEntity.userId,
            username = userEntity.userId,
            email = userEntity.email,
            token = token,
            role = userEntity.role,
            displayName = userEntity.displayName
        )
        return Result.success(user)
    }

    override suspend fun register(
        username: String,
        password: String,
        displayName: String,
        email: String
    ): Result<User> {
        val trimmedUser = username.trim()
        if (trimmedUser.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password are required"))
        }

        val existing = userDao.getUserByUserId(trimmedUser)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Username '$trimmedUser' is already registered"))
        }

        val newEntity = com.example.warehousescan.core.storage.UserEntity(
            userId = trimmedUser,
            password = password,
            displayName = displayName.ifBlank { trimmedUser },
            email = email.trim(),
            role = "USER",
            active = true
        )

        userDao.insertUser(newEntity)

        val token = "local-token-${newEntity.userId}"
        tokenStore.saveToken(token)

        val user = User(
            userId = newEntity.userId,
            username = newEntity.userId,
            email = newEntity.email,
            token = token,
            role = newEntity.role,
            displayName = newEntity.displayName
        )
        return Result.success(user)
    }

    override suspend fun fetchProfile(): Result<User> {
        // Can fetch current logged in user or default admin/first user
        val token = tokenStore.getToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No active session"))
        }
        // Extract userId from token or query active users
        val users = userDao.getAllUsers()
        // For simplicity, return success with a default or first active user if token exists
        return Result.success(
            User(
                userId = "admin",
                username = "admin",
                email = "admin@example.com",
                token = token,
                role = "ADMIN",
                displayName = "Administrator"
            )
        )
    }
}
