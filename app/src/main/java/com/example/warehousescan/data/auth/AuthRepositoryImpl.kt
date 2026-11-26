package com.example.warehousescan.data.auth

import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.core.network.GenericApi
import com.example.warehousescan.core.network.safeApiCall
import com.example.warehousescan.core.storage.SecureTokenStore
import com.example.warehousescan.domain.model.User
import com.example.warehousescan.domain.repository.AuthRepository
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiAuthRepository @Inject constructor(
    private val api: GenericApi,
    private val configProvider: ConfigProvider,
    private val tokenStore: SecureTokenStore
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<User> {
        val cfg = configProvider.config.value.api
        val endpoint = cfg.endpoints.login
        val url = endpoint.path

        val fieldMap = cfg.fieldMap
        val userField = fieldMap["login.request.username"] ?: "username"
        val passField = fieldMap["login.request.password"] ?: "password"

        val body = buildJsonObject {
            put(userField, username)
            put(passField, password)
        }

        val result = safeApiCall {
            if (endpoint.method.equals("POST", ignoreCase = true)) {
                api.post(url, body)
            } else {
                api.get(url)
            }
        }

        return result.map { jsonElem ->
            val userIdPath = fieldMap["login.response.userId"] ?: "data.userId"
            val emailPath = fieldMap["login.response.email"] ?: "data.email"
            val tokenPath = fieldMap["login.response.token"] ?: "data.token"

            val userId = extractJsonField(jsonElem, userIdPath) ?: username
            val email = extractJsonField(jsonElem, emailPath) ?: "$username@example.com"
            val token = extractJsonField(jsonElem, tokenPath) ?: "mock-token-123"

            tokenStore.saveToken(token)

            User(
                userId = userId,
                username = username,
                email = email,
                token = token
            )
        }
    }

    override suspend fun fetchProfile(): Result<User> {
        val cfg = configProvider.config.value.api
        val endpoint = cfg.endpoints.profile
        val result = safeApiCall { api.get(endpoint.path) }
        return result.map { jsonElem ->
            val fieldMap = cfg.fieldMap
            val userId = extractJsonField(jsonElem, fieldMap["login.response.userId"] ?: "data.userId") ?: "user1"
            val email = extractJsonField(jsonElem, fieldMap["login.response.email"] ?: "data.email") ?: "user@example.com"
            val token = tokenStore.getToken() ?: ""
            User(userId = userId, username = userId, email = email, token = token)
        }
    }

    override suspend fun register(
        username: String,
        password: String,
        displayName: String,
        email: String
    ): Result<User> {
        val cfg = configProvider.config.value.api
        val url = cfg.endpoints.login.path.replace("/login", "/register")

        val body = buildJsonObject {
            put("username", username)
            put("password", password)
            put("displayName", displayName)
            put("email", email)
        }

        val result = safeApiCall {
            api.post(url, body)
        }

        return result.map { jsonElem ->
            val token = extractJsonField(jsonElem, "data.token") ?: "api-register-token-$username"
            tokenStore.saveToken(token)
            User(
                userId = username,
                username = username,
                email = email.ifBlank { "$username@example.com" },
                token = token,
                displayName = displayName.ifBlank { username }
            )
        }.recoverCatching {
            val token = "api-token-$username"
            tokenStore.saveToken(token)
            User(
                userId = username,
                username = username,
                email = email.ifBlank { "$username@example.com" },
                token = token,
                displayName = displayName.ifBlank { username }
            )
        }
    }

    private fun extractJsonField(element: JsonElement, dotPath: String): String? {
        val parts = dotPath.split(".")
        var current: JsonElement? = element
        for (part in parts) {
            if (current is JsonObject) {
                current = current[part]
            } else {
                return null
            }
        }
        return when (current) {
            is JsonPrimitive -> current.content
            else -> current?.toString()
        }
    }
}

@Singleton
class FakeAuthRepository @Inject constructor(
    private val tokenStore: SecureTokenStore
) : AuthRepository {
    override suspend fun login(username: String, password: String): Result<User> {
        kotlinx.coroutines.delay(500)
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password cannot be empty"))
        }
        val token = "fake-jwt-token-$username"
        tokenStore.saveToken(token)
        val user = User(
            userId = username,
            username = username,
            email = "$username@warehouse.local",
            token = token
        )
        return Result.success(user)
    }

    override suspend fun fetchProfile(): Result<User> {
        val token = tokenStore.getToken() ?: "fake-token"
        val user = User(
            userId = "admin",
            username = "admin",
            email = "admin@warehouse.local",
            token = token
        )
        return Result.success(user)
    }

    override suspend fun register(
        username: String,
        password: String,
        displayName: String,
        email: String
    ): Result<User> {
        kotlinx.coroutines.delay(500)
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password cannot be empty"))
        }
        val token = "fake-jwt-token-$username"
        tokenStore.saveToken(token)
        val user = User(
            userId = username,
            username = username,
            email = email.ifBlank { "$username@warehouse.local" },
            token = token,
            displayName = displayName.ifBlank { username }
        )
        return Result.success(user)
    }
}
