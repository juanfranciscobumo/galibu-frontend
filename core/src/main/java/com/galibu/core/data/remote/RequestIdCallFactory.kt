package com.galibu.core.data.remote

import okhttp3.Call
import okhttp3.Request

/**
 * Añade los headers de trazabilidad a cada llamada OkHttp. Se ejecuta en el
 * hilo que inicia la llamada de Retrofit, por lo que puede leer el request id
 * que dejó [RequestTrace.withTransaction] en el contexto de la corrutina.
 */
class RequestIdCallFactory(
    private val delegate: Call.Factory
) : Call.Factory {

    override fun newCall(request: Request): Call {
        val builder = request.newBuilder()
            .header("X-Request-ID", RequestTrace.currentId() ?: RequestTrace.newId())
            .header("X-Client-Platform", "android")
        RequestTrace.appName?.takeIf { it.isNotBlank() }?.let { builder.header("X-Client-App", it) }
        RequestTrace.appVersion?.takeIf { it.isNotBlank() }?.let { builder.header("X-Client-Version", it) }
        RequestTrace.device?.takeIf { it.isNotBlank() }?.let { builder.header("X-Client-Device", it) }
        return delegate.newCall(builder.build())
    }
}
