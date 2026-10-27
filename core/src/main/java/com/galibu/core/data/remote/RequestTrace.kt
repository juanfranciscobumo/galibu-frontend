package com.galibu.core.data.remote

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Correlación de peticiones por transacción.
 *
 * El frontend genera un id por operación de negocio y lo comparte con todas las
 * llamadas HTTP que ocurren dentro de [withTransaction]. El backend recibe el
 * header X-Request-ID, agrupa las llamadas y las expone en el dashboard.
 */
object RequestTrace {

    private val threadId = ThreadLocal<String?>()

    @Volatile
    var appName: String? = null
        private set

    @Volatile
    var appVersion: String? = null
        private set

    @Volatile
    var device: String? = null
        private set

    @Volatile
    private var configured = false

    fun configure(appName: String, appVersion: String, device: String) {
        this.appName = appName
        this.appVersion = appVersion
        this.device = device
        configured = true
    }

    fun currentId(): String? = threadId.get()

    /** Contexto para que un `launch` secundario conserve la transacción actual. */
    fun currentContext(): CoroutineContext {
        val id = currentId()
        return if (id.isNullOrBlank()) EmptyCoroutineContext else RequestIdElement(id)
    }

    fun newId(): String = "trx_" + UUID.randomUUID().toString().replace("-", "").take(24)

    internal fun setThreadId(id: String?) {
        if (id == null) threadId.remove() else threadId.set(id)
    }

    /**
     * Si ya hay una transacción en curso, la reutiliza. Si no, abre una nueva
     * que se propaga al contexto de corrutina (incluye cambios de dispatcher).
     */
    suspend fun <T> withTransaction(block: suspend () -> T): T {
        if (!currentId().isNullOrBlank()) {
            return block()
        }
        return withContext(RequestIdElement(newId())) { block() }
    }
}

private class RequestIdElement(
    private val id: String
) : ThreadContextElement<String?>, AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<RequestIdElement>

    override fun updateThreadContext(context: CoroutineContext): String? {
        val previous = RequestTrace.currentId()
        RequestTrace.setThreadId(id)
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: String?) {
        RequestTrace.setThreadId(oldState)
    }
}
