package com.example.warehousescan.core.network

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface GenericApi {
    @POST
    suspend fun post(@Url url: String, @Body body: JsonElement): Response<JsonElement>

    @GET
    suspend fun get(@Url url: String): Response<JsonElement>

    @Multipart
    @POST
    suspend fun postMultipart(
        @Url url: String,
        @Part parts: List<MultipartBody.Part>
    ): Response<JsonElement>
}
