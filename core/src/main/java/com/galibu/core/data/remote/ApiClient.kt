package com.galibu.core.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

object ApiClient {

    fun moshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Log de red: método + URL, headers, request y response completos (tag "OkHttp").
    // El header Authorization se redacta para no filtrar el token en logcat.
    private fun networkLogging(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }

    private fun bareService(): AuthRefreshApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(networkLogging())
            .build()
        return Retrofit.Builder()
            .baseUrl(ApiConfig.baseUrl)
            .callFactory(RequestIdCallFactory(client))
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create()
    }

    /** Cliente sin JWT para que las apps envíen logs incluso antes de autenticarse. */
    fun telemetryService(): TelemetryApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(networkLogging())
            .build()
        return Retrofit.Builder()
            .baseUrl(ApiConfig.baseUrl)
            .callFactory(RequestIdCallFactory(client))
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create()
    }

    fun authedService(store: TokenStore): ApiService {
        val bare = bareService()
        val client = OkHttpClient.Builder()
            .addInterceptor(networkLogging())
            .addInterceptor { chain ->
                val original = chain.request()
                val token = store.accessToken()
                val request = if (token != null) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .authenticator(TokenAuthenticator(store) { refreshToken ->
                try {
                    val res = bare.refresh(RefreshRequestDto(refreshToken)).execute()
                    if (res.isSuccessful) res.body() else null
                } catch (e: Exception) {
                    null
                }
            })
            .build()
        return Retrofit.Builder()
            .baseUrl(ApiConfig.baseUrl)
            .callFactory(RequestIdCallFactory(client))
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create()
    }
}
