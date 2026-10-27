package com.galibu.core.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

/** Endpoint público para que las apps reporten eventos y errores al monitor. */
interface TelemetryApi {

    @POST("front-logs")
    suspend fun send(@Body entries: List<FrontLogDto>)
}

@JsonClass(generateAdapter = true)
data class FrontLogDto(
    val at: Long,
    val level: String,
    val app: String,
    val tag: String,
    val message: String,
    val requestId: String? = null,
    val userId: String? = null,
    val role: String? = null,
    val device: String? = null,
    val version: String? = null,
    val screen: String? = null,
    val event: String? = null,
    val metadata: Map<String, String>? = null
)
