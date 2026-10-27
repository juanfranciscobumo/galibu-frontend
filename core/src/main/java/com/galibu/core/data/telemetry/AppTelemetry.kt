package com.galibu.core.data.telemetry

import android.os.Build
import android.util.Log
import com.galibu.core.data.remote.ApiClient
import com.galibu.core.data.remote.FrontLogDto
import com.galibu.core.data.remote.RequestTrace
import com.galibu.core.data.remote.TelemetryApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Logger ligero del frontend. Acumula eventos y los envía en lote al backend
 * para alimentar la pestaña "Logs front" y las estadísticas de uso.
 */
object AppTelemetry {

    private const val TAG = "AppTelemetry"
    private const val MAX_QUEUE = 500
    private const val BATCH_SIZE = 40
    private const val FLUSH_INTERVAL_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = ConcurrentLinkedQueue<FrontLogDto>()
    private val sendLock = Mutex()

    @Volatile
    private var api: TelemetryApi? = null

    @Volatile
    private var initialized = false

    @Volatile
    private var appName = "galibu"

    @Volatile
    private var appVersion = ""

    @Volatile
    private var device = ""

    @Volatile
    private var currentUserId: String? = null

    @Volatile
    private var currentRole: String? = null

    fun init(appName: String, appVersion: String) {
        if (initialized) {
            return
        }
        initialized = true
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                error("crash", "Uncaught exception on ${'$'}{thread.name}", throwable)
                runBlocking { flushInternal() }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo vaciar la telemetría en el crash: ${'$'}{e.message}")
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
        this.appName = appName
        this.appVersion = appVersion
        this.device = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
        RequestTrace.configure(appName, appVersion, device)
        api = runCatching { ApiClient.telemetryService() }
            .onFailure { Log.w(TAG, "No se pudo crear el servicio de telemetría: ${it.message}") }
            .getOrNull()
        scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flushInternal()
            }
        }
    }

    fun setUser(userId: String?, role: String?) {
        currentUserId = userId
        currentRole = role
    }

    fun screen(name: String) {
        enqueue(
            level = "INFO",
            tag = "nav",
            message = "Pantalla: $name",
            screen = name
        )
    }

    fun event(name: String, metadata: Map<String, String> = emptyMap()) {
        enqueue(
            level = "EVENT",
            tag = "app",
            message = name,
            event = name,
            metadata = metadata
        )
    }

    fun info(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        enqueue(level = "INFO", tag = tag, message = message, metadata = metadata)
    }

    fun warn(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        enqueue(level = "WARN", tag = tag, message = message, metadata = metadata)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val detail = if (throwable == null) {
            message
        } else {
            "$message · ${throwable.javaClass.simpleName}: ${throwable.message}"
        }
        enqueue(level = "ERROR", tag = tag, message = detail)
    }

    fun flush() {
        scope.launch { flushInternal() }
    }

    private fun enqueue(
        level: String,
        tag: String,
        message: String,
        screen: String? = null,
        event: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        val entry = FrontLogDto(
            at = System.currentTimeMillis(),
            level = level,
            app = appName,
            tag = tag,
            message = message.take(4000),
            requestId = RequestTrace.currentId(),
            userId = currentUserId,
            role = currentRole,
            device = device,
            version = appVersion,
            screen = screen,
            event = event,
            metadata = metadata.ifEmpty { null }
        )
        if (queue.size >= MAX_QUEUE) {
            queue.poll()
        }
        queue.offer(entry)
        if (queue.size >= BATCH_SIZE) {
            scope.launch { flushInternal() }
        }
    }

    private suspend fun flushInternal() {
        if (!sendLock.tryLock()) {
            return
        }
        try {
            val service = api ?: return
            val batch = ArrayList<FrontLogDto>(BATCH_SIZE)
            while (batch.size < BATCH_SIZE) {
                batch.add(queue.poll() ?: break)
            }
            if (batch.isEmpty()) {
                return
            }
            try {
                service.send(batch)
            } catch (e: Exception) {
                // Si falla la red, devolvemos el lote para el próximo intento.
                batch.asReversed().forEach {
                    if (queue.size < MAX_QUEUE) {
                        queue.offer(it)
                    }
                }
                Log.w(TAG, "No se pudieron enviar ${batch.size} logs: ${e.message}")
            }
        } finally {
            sendLock.unlock()
        }
    }
}
