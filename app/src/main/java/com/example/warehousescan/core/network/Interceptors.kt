package com.example.warehousescan.core.network

import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.core.storage.SecureTokenStore
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class DynamicBaseUrlInterceptor @Inject constructor(
    private val configProvider: ConfigProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val baseStr = configProvider.config.value.api.baseUrl
        val base = baseStr.toHttpUrl()
        val req = chain.request()
        val newUrl = req.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(req.newBuilder().url(newUrl).build())
    }
}

class AuthHeaderInterceptor @Inject constructor(
    private val configProvider: ConfigProvider,
    private val tokenStore: SecureTokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val cfg = configProvider.config.value.api
        val token = tokenStore.getToken()
        val req = chain.request()
        if (!token.isNullOrBlank()) {
            val authValue = "${cfg.authHeaderPrefix}$token"
            val newReq = req.newBuilder()
                .header(cfg.authHeaderName, authValue)
                .build()
            return chain.proceed(newReq)
        }
        return chain.proceed(req)
    }
}
