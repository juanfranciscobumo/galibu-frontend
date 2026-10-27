package com.galibu.client.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.galibu.core.data.model.*
import com.galibu.client.ui.util.BidPushHelper
import com.galibu.core.ui.viewmodels.SharedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientViewModel(application: Application) : SharedViewModel(application) {

    /** Push del sistema ante aceptar/iniciar/finalizar/calificar (abre el detalle). */
    override fun dispatchEventPush(notif: com.galibu.core.data.model.AppNotification) {
        try {
            BidPushHelper.showEventPush(
                getApplication(),
                notif.title,
                notif.body,
                notif.jobId,
                com.galibu.client.MainActivity::class.java
            )
        } catch (e: Exception) {
            Log.e("ClientViewModel", "Falla mostrar push de evento: ${e.localizedMessage}")
        }
    }

    /**
     * Ofertas por solicitud del cliente (jobId -> cantidad).
     * Se alimenta de los bids locales (ver [refreshClientBids]).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val clientBidsByJob: StateFlow<Map<String, Int>> = combine(allJobs, currentUser) { jobs, user ->
        jobs.filter { it.clientId == user?.id && !it.isDirectChat() }.map { it.id }
    }.flatMapLatest { jobIds ->
        if (jobIds.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(jobIds.map { id ->
                repository.getBidsForJob(id).map { bids -> id to bids.size }
            }) { pairs -> pairs.toMap() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Baja del backend las ofertas de cada solicitud propia (para el contador y el detalle).
     * Detecta ofertas nuevas y genera notificación in-app (campanita) + push del sistema
     * con el detalle de la oferta.
     */
    fun refreshClientBids(jobIds: List<String>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            jobIds.forEach { id ->
                try {
                    val before = repository.getBidsForJobSync(id).map { it.id }.toSet()
                    repository.refreshBidsForJob(id)
                    val after = repository.getBidsForJobSync(id)
                    val userId = currentUser.value?.id
                    val job = allJobs.value.find { it.id == id }
                    after.filter { it.id !in before }.forEach { bid ->
                        if (BidPushHelper.isAlreadyNotified(context, bid.id)) return@forEach
                        BidPushHelper.markBidNotified(context, bid.id)
                        // Solo ofertas recientes: evita spam de bids viejos en el primer sync.
                        val createdMs = if (bid.createdAt < 1_000_000_000_000L) bid.createdAt * 1000 else bid.createdAt
                        val isRecent = System.currentTimeMillis() - createdMs < 45 * 60 * 1000L
                        if (isRecent && userId != null && job != null && job.clientId == userId &&
                            bid.professionalId != userId
                        ) {
                            val amountText = java.text.NumberFormat
                                .getNumberInstance(java.util.Locale.US)
                                .format(bid.amount)
                            val notif = repository.notifyNewBidOffer(userId, job, bid, amountText)
                            BidPushHelper.showNewOfferPush(context, notif.title, notif.body, job.id)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ClientViewModel", "Falla refrescar ofertas de $id: ${e.localizedMessage}")
                }
            }
        }
    }

    fun createJobRequest(category: String, title: String, description: String, budget: Double, address: String, department: String = "Bogotá D.C.", municipality: String = "Bogotá", suggestedPrice: Double? = null,
        photos: List<String>? = null) {
        viewModelScope.launch {
            try {
                showLoading("Publicando solicitud de servicio en Galibu...")
                kotlinx.coroutines.delay(1200)
                repository.createJobRequest(category, title, description, budget, address, department, municipality, suggestedPrice, photos = photos)
            } catch (e: Exception) {
                val user = currentUser.value
                val warning = AppNotification(
                    userId = user?.id ?: "",
                    title = "Trabajo Creado Localmente 🛠️",
                    body = "Tu solicitud se guardó en el dispositivo, pero no se sincronizó a Firebase debido a: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
            } finally {
                hideLoading()
            }
        }
    }

    fun createJobRequest(
        category: String,
        title: String,
        description: String,
        budget: Double,
        address: String,
        department: String = "Bogotá D.C.",
        municipality: String = "Bogotá",
        suggestedPrice: Double? = null,
        professionalId: String? = null,
        professionalName: String? = null,
        source: String = "PUBLISH",
        photos: List<String>? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                showLoading("Publicando solicitud...")
                kotlinx.coroutines.delay(1000)
                repository.createJobRequest(category, title, description, budget, address, department, municipality, suggestedPrice, professionalId, professionalName, source, photos = photos)
                hideLoading()
                onSuccess()
            } catch (e: Exception) {
                hideLoading()
                val message = e.localizedMessage ?: ""
                val isConflict = message.contains("409") || message.contains("Conflict", ignoreCase = true) ||
                    message.contains("activa con esta ubicaci", ignoreCase = true)
                onError(
                    if (isConflict) "Ya tienes una solicitud activa con esta ubicación y categoría. Revísala en Mis solicitudes."
                    else "No se pudo publicar la solicitud. Verifica tu conexión."
                )
                Log.e("ClientViewModel", "Error creando solicitud: ${e.localizedMessage}")
            }
        }
    }

    fun startChatWithProfessional(
        proId: String,
        proName: String,
        proSpecialty: String,
        jobId: String? = null,
        onStarted: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                showLoading("Iniciando chat...")
                val resolved = repository.resolveChatJobId(proId, proName, proSpecialty, jobId)
                hideLoading()
                if (resolved != null) {
                    onStarted(resolved)
                } else {
                    showAlert("Aviso", "No se pudo iniciar el chat. Verifica tu conexión e inténtalo nuevamente.", true)
                }
            } catch (e: Exception) {
                hideLoading()
                Log.e("ClientViewModel", "Error iniciando chat: ${e.localizedMessage}")
                showError("No se pudo iniciar el chat. Verifica tu conexión e inténtalo nuevamente.")
            }
        }
    }

    fun hireProfessional(proId: String, proName: String, proSpecialty: String, hourlyRate: Double?, proBaseHourlyRate: Double? = null, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                showLoading("Enviando solicitud de contratación...")
                val jobId = repository.hireProfessional(proId, proName, proSpecialty, hourlyRate, proBaseHourlyRate)
                hideLoading()
                if (jobId != null) {
                    onResult(jobId)
                }
            } catch (e: Exception) {
                hideLoading()
                Log.e("ClientViewModel", "Error al contratar profesional: ${e.localizedMessage}")
                showError("No se pudo enviar la solicitud de contratación. Verifica tu conexión e inténtalo nuevamente.")
            }
        }
    }

    fun acceptBid(bidId: String) {
        viewModelScope.launch {
            try {
                showLoading("Aceptando cotización de servicio...")
                kotlinx.coroutines.delay(1200)
                repository.acceptBid(bidId)
                showSuccess("Oferta aceptada correctamente.")
            } catch (e: Exception) {
                Log.e("ClientViewModel", "Falla aceptar propuesta: ${e.localizedMessage}")
                val user = currentUser.value
                val warning = AppNotification(
                    userId = user?.id ?: "",
                    title = "Propuesta Aceptada Localmente ⏳",
                    body = "Se aceptó la propuesta de forma offline. Falló la sincronización con Firebase: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
                showWarning("La oferta se procesó localmente y se sincronizará automáticamente al recuperar la conexión.")
            } finally {
                hideLoading()
            }
        }
    }

    fun rejectBid(bidId: String) {
        viewModelScope.launch {
            try {
                showLoading("Rechazando cotización de servicio...")
                kotlinx.coroutines.delay(1000)
                repository.rejectBid(bidId)
                showSuccess("Oferta rechazada.")
            } catch (e: Exception) {
                Log.e("ClientViewModel", "Falla rechazar propuesta: ${e.localizedMessage}")
                val user = currentUser.value
                val warning = AppNotification(
                    userId = user?.id ?: "",
                    title = "Propuesta Rechazada Localmente ⏳",
                    body = "Se rechazó la propuesta de forma offline. Falló la sincronización con Firebase: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
                showWarning("El rechazo se procesó localmente y se sincronizará automáticamente al recuperar la conexión.")
            } finally {
                hideLoading()
            }
        }
    }

    /** Refuerza la lista de ofertas del job desde el backend (se usa tras aceptar/rechazar). */
    fun refreshSelectedJobBids(jobId: String) {
        viewModelScope.launch {
            repository.refreshBidsForJob(jobId)
        }
    }

    fun proposeCounterOffer(bidId: String, amount: Double) {
        viewModelScope.launch {
            try {
                showLoading("Proponiendo nueva tarifa de negociación...")
                kotlinx.coroutines.delay(1000)
                repository.proposeCounterOffer(bidId, amount)
                showSuccess("Contraoferta enviada al profesional.")
            } catch (e: Exception) {
                Log.e("ClientViewModel", "Falla contraoferta: ${e.localizedMessage}")
                val user = currentUser.value
                val warning = AppNotification(
                    userId = user?.id ?: "",
                    title = "Contraoferta Guardada Localmente 🔄",
                    body = "Tu contraoferta de $$amount COP se guardó de forma offline. Falló sincronización Firebase: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
                showWarning("La contraoferta se guardó localmente y se sincronizará automáticamente al recuperar la conexión.")
            } finally {
                hideLoading()
            }
        }
    }

    fun cancelJob(jobId: String, reason: String = "", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Cancelando solicitud...")
                kotlinx.coroutines.delay(800)
                repository.cancelJob(jobId, reason)
                onComplete()
            } catch (e: Exception) {
                Log.e("ClientViewModel", "Falla cancelar solicitud: ${e.localizedMessage}")
                showError("No se pudo cancelar la solicitud. Verifica tu conexión e inténtalo nuevamente.")
            } finally {
                hideLoading()
            }
        }
    }

    fun acceptDirectHire(jobId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Aceptando contratación directa...")
                kotlinx.coroutines.delay(1000)
                repository.acceptDirectHire(jobId)
                hideLoading()
                onComplete()
            } catch (e: Exception) {
                hideLoading()
                Log.e("ClientViewModel", "Falla aceptar contratación: ${e.localizedMessage}")
                showAlert("Error", "No se pudo aceptar la contratación: ${e.localizedMessage}")
            }
        }
    }

    fun submitCounterOfferDirectHire(jobId: String, amount: Double, isClient: Boolean = true, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Enviando contraoferta...")
                kotlinx.coroutines.delay(1000)
                repository.submitCounterOfferDirectHire(jobId, amount, isClient)
                hideLoading()
                onComplete()
            } catch (e: Exception) {
                hideLoading()
                Log.e("ClientViewModel", "Falla contraoferta directa: ${e.localizedMessage}")
                showAlert("Error", "No se pudo enviar la contraoferta: ${e.localizedMessage}")
            }
        }
    }

    /** El cliente también puede finalizar el servicio y calificar al profesional. */
    fun finishService(jobId: String, stars: Int, comment: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Finalizando servicio...")
                kotlinx.coroutines.delay(800)
                repository.finishService(jobId, stars, comment)
                repository.refreshJobDetails(jobId)
                hideLoading()
                onComplete()
            } catch (e: Exception) {
                hideLoading()
                Log.e("ClientViewModel", "Falla finalizar servicio: ${e.localizedMessage}")
                showAlert("Aviso", "No se pudo finalizar el servicio: ${e.localizedMessage}", true)
            }
        }
    }
}

