package com.example.warehousescan.core.network

import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.core.storage.SecureTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        configProvider: ConfigProvider,
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
        authHeaderInterceptor: AuthHeaderInterceptor
    ): OkHttpClient {
        val cfg = configProvider.config.value.api
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(cfg.connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(cfg.readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(cfg.writeTimeoutSec, TimeUnit.SECONDS)
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        configProvider: ConfigProvider
    ): Retrofit {
        val baseUrl = configProvider.config.value.api.baseUrl
        val contentType = "application/json; charset=UTF8".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideGenericApi(retrofit: Retrofit): GenericApi {
        return retrofit.create(GenericApi::class.java)
    }
}
