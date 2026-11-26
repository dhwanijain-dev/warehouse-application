package com.example.warehousescan.core.network

import kotlinx.serialization.json.JsonElement
import retrofit2.Response

sealed class AppError : Exception() {
    data class Network(override val cause: Throwable?) : AppError()
    data class Unauthorized(override val message: String? = "Unauthorized") : AppError()
    data class Server(val code: Int, override val message: String?) : AppError()
    data class Validation(override val message: String?) : AppError()
    data class Unknown(override val cause: Throwable?) : AppError()
}

suspend fun safeApiCall(block: suspend () -> Response<JsonElement>): Result<JsonElement> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.success(body)
            } else {
                Result.failure(AppError.Unknown(IllegalStateException("Response body is null")))
            }
        } else {
            val code = response.code()
            if (code == 401) {
                Result.failure(AppError.Unauthorized())
            } else {
                Result.failure(AppError.Server(code, response.errorBody()?.string()))
            }
        }
    } catch (e: java.io.IOException) {
        Result.failure(AppError.Network(e))
    } catch (e: Exception) {
        Result.failure(AppError.Unknown(e))
    }
}
