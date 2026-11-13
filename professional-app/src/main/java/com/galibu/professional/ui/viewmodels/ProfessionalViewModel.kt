package com.galibu.professional.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.galibu.core.data.model.*
import com.galibu.core.data.remote.BoldCheckoutResponse
import com.galibu.core.ui.viewmodels.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ProfessionalViewModel(application: Application) : SharedViewModel(application) {

    private val _boldCheckout = MutableStateFlow<BoldCheckoutResponse?>(null)
    val boldCheckout: StateFlow<BoldCheckoutResponse?> = _boldCheckout.asStateFlow()

    /** Se dispara cuando el backend exige Galibu Pro (HTTP 402) para abrir el paywall. */
    private val _paywallRequested = MutableStateFlow(false)
    val paywallRequested: StateFlow<Boolean> = _paywallRequested.asStateFlow()

    fun openPaywall() {
        _paywallRequested.value = true
    }

    fun dismissPaywall() {
        _paywallRequested.value = false
    }

    /** Crea el link de pago de Bold para activar o renovar Galibu Pro. */
    fun startPlanCheckout(plan: String) {
        viewModelScope.launch {
            try {
                showLoading("Conectando con Bold...")
                val checkout = repository.createSubscriptionCheckout(plan)
                if (checkout.checkoutUrl.isBlank()) {
                    showError("Bold no devolvió un link de pago. Inténtalo de nuevo.")
                    return@launch
                }
                _boldCheckout.value = checkout
            } catch (e: Exception) {
                Log.e("ProfessionalViewModel", "Falla creando checkout de suscripción: ${e.localizedMessage}")
                val detail = e.localizedMessage ?: ""
                val message = when {
                    detail.contains("503") -> "La pasarela de pagos Bold no está configurada en el servidor."
                    detail.contains("401") || detail.contains("403") ->
                        "Las llaves de Bold no son válidas o están inactivas."
                    else -> "No se pudo conectar con Bold. Verifica tu conexión e inténtalo de nuevo."
                }
                showError(message)
            } finally {
                hideLoading()
            }
        }
    }

    fun closeBoldCheckout() {
        _boldCheckout.value = null
    }

    /**
     * Cierra el checkout y consulta la orden al backend hasta que Bold confirme.
     * El backend activa la suscripción una sola vez (polling y webhook son idempotentes).
     */
    fun confirmBoldPayment() {
        val checkout = _boldCheckout.value ?: return
        _boldCheckout.value = null

        viewModelScope.launch {
            try {
                showLoading("Confirmando pago con Bold...")
                repeat(10) {
                    val result = repository.boldOrderStatus(checkout.orderId)
                    when (result.status.uppercase(Locale.US)) {
                        "PAID" -> {
                            loadSubscription()
                            repository.refreshMe()
                            showSuccess("¡Galibu Pro activado! Ya puedes seguir tomando trabajos.")
                            return@launch
                        }
                        "REJECTED", "CANCELLED", "EXPIRED" -> {
                            val status = result.status.lowercase(Locale.US)
                            showError("El pago no se completó ($status). No se realizó ningún cobro.")
                            return@launch
                        }
                    }
                    kotlinx.coroutines.delay(3000)
                }
                showWarning(
                    "No recibimos la confirmación del pago. Si ya pagaste, el plan se activará " +
                        "automáticamente en unos minutos."
                )
            } catch (e: Exception) {
                Log.e("ProfessionalViewModel", "Falla confirmando pago Bold: ${e.localizedMessage}")
                showError("No se pudo confirmar el pago con Bold. Inténtalo de nuevo.")
            } finally {
                hideLoading()
            }
        }
    }

    /** Push del sistema ante oferta aceptada / cliente que finaliza / servicio completado. */
    override fun dispatchEventPush(notif: com.galibu.core.data.model.AppNotification) {
        try {
            com.galibu.professional.ui.util.ProPushHelper.showEventPush(
                notif.title,
                notif.body,
                getApplication(),
                tag = "${notif.jobId ?: notif.title}:${notif.timestamp}"
            )
        } catch (e: Exception) {
            Log.e("ProfessionalViewModel", "Falla mostrar push de evento: ${e.localizedMessage}")
        }
    }

    /** Push del sistema para eventos de la suscripción. */
    override fun dispatchWalletPush(notif: com.galibu.core.data.model.AppNotification) {
        try {
            com.galibu.professional.ui.util.ProPushHelper.showWalletPush(
                notif.title,
                notif.body,
                getApplication(),
                tag = "wallet:${notif.timestamp}"
            )
        } catch (e: Exception) {
            Log.e("ProfessionalViewModel", "Falla mostrar push de billetera: ${e.localizedMessage}")
        }
    }

    private fun handleProError(msg: String, fallback: String) {
        when {
            msg.contains("402") || msg.contains("Galibu Pro", ignoreCase = true) ||
                msg.contains("servicios gratis", ignoreCase = true) -> {
                _paywallRequested.value = true
                showWarning(
                    "Usaste tus 3 servicios gratis. Activa Galibu Pro para seguir tomando trabajos.",
                    "Galibu Pro"
                )
            }
            else -> showError(fallback)
        }
    }

    fun submitBid(jobId: String, amount: Double, comment: String, durationHours: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Enviando tu propuesta de servicio...")
                kotlinx.coroutines.delay(1000)
                repository.submitBid(jobId, amount, comment, durationHours)
                showSuccess("Oferta enviada al cliente.")
                onComplete()
            } catch (e: Exception) {
                Log.e("ProfessionalViewModel", "Falla enviar propuesta: ${e.localizedMessage}")
                val msg = e.localizedMessage ?: ""
                when {
                    msg.contains("403") || msg.contains("Solo un profesional", ignoreCase = true) ->
                        showError("Solo un perfil profesional puede enviar ofertas.")
                    msg.contains("402") || msg.contains("Galibu Pro", ignoreCase = true) ||
                        msg.contains("servicios gratis", ignoreCase = true) ->
                        handleProError(msg, "No se pudo enviar la oferta.")
                    msg.contains("409") || msg.contains("propia solicitud", ignoreCase = true) ->
                        showError("No puedes ofertar en tu propia solicitud.")
                    msg.contains("409") || msg.contains("no admite ofertas", ignoreCase = true) ->
                        showError("La solicitud ya no admite ofertas en su estado actual.")
                    msg.contains("Unable to resolve host", ignoreCase = true) ||
                        msg.contains("timeout", ignoreCase = true) ||
                        msg.contains("IOException", ignoreCase = true) ->
                        showError("Sin conexión: no se pudo enviar la oferta. Inténtalo de nuevo.")
                    else -> showError("No se pudo enviar la oferta: ${msg.ifBlank { "error desconocido" }}")
                }
            } finally {
                hideLoading()
            }
        }
    }

    /** El profesional cotiza (envía su oferta) en una contratación directa (job NEGOTIATING asignado). */
    fun submitBidForDirectJob(
        jobId: String,
        amount: Double,
        comment: String,
        durationHours: Int,
        onResult: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                showLoading("Enviando cotización...")
                kotlinx.coroutines.delay(1000)
                repository.submitBid(jobId, amount, comment, durationHours)
                repository.refreshMe()
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                val msg = e.localizedMessage ?: ""
                handleProError(
                    msg,
                    "No se pudo enviar la cotización. Verifica tu conexión e inténtalo nuevamente."
                )
                Log.e("ProfessionalViewModel", "Error al enviar cotización directa: ${e.localizedMessage}")
            }
        }
    }

    /** El profesional declina una invitación de contratación directa (cancela el job). */
    fun declineDirectJob(jobId: String, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Declinando invitación...")
                kotlinx.coroutines.delay(1000)
                repository.cancelJob(jobId, "El profesional declinó la invitación")
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                Log.e("ProfessionalViewModel", "Error al declinar invitación: ${e.localizedMessage}")
                showError("No se pudo declinar la invitación. Verifica tu conexión e inténtalo nuevamente.")
            }
        }
    }

    fun startService(jobId: String, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Iniciando servicio...")
                kotlinx.coroutines.delay(800)
                repository.startService(jobId)
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                showError("No se pudo iniciar el servicio. Verifica tu conexión e inténtalo nuevamente.")
                Log.e("ProfessionalViewModel", "Error al iniciar servicio: ${e.localizedMessage}")
            }
        }
    }

    fun finishService(jobId: String, stars: Int?, comment: String?, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Finalizando servicio...")
                kotlinx.coroutines.delay(800)
                repository.finishService(jobId, stars, comment)
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                showError("No se pudo finalizar el servicio. Verifica tu conexión e inténtalo nuevamente.")
                Log.e("ProfessionalViewModel", "Error al finalizar servicio: ${e.localizedMessage}")
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
                Log.e("ProfessionalViewModel", "Falla cancelar solicitud: ${e.localizedMessage}")
                showError("No se pudo cancelar la solicitud. Verifica tu conexión e inténtalo nuevamente.")
            } finally {
                hideLoading()
            }
        }
    }

    fun acceptBid(bidId: String) {
        viewModelScope.launch {
            try {
                showLoading("Aceptando cotización de servicio...")
                kotlinx.coroutines.delay(1200)
                repository.acceptBid(bidId)
                repository.refreshMe()
                showSuccess("Oferta aceptada correctamente.")
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: ""
                handleProError(
                    msg,
                    "No se pudo aceptar la oferta. Verifica tu conexión e inténtalo nuevamente."
                )
                Log.e("ProfessionalViewModel", "Falla aceptar propuesta: ${e.localizedMessage}")
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
                Log.e("ProfessionalViewModel", "Falla rechazar propuesta: ${e.localizedMessage}")
                showError("No se pudo rechazar la oferta. Verifica tu conexión e inténtalo nuevamente.")
            } finally {
                hideLoading()
            }
        }
    }

    fun proposeCounterOffer(bidId: String, amount: Double) {
        viewModelScope.launch {
            try {
                showLoading("Proponiendo nueva tarifa de negociación...")
                kotlinx.coroutines.delay(1000)
                repository.proposeCounterOffer(bidId, amount)
                showSuccess("Contraoferta enviada al cliente.")
            } catch (e: Exception) {
                Log.e("ProfessionalViewModel", "Falla contraoferta: ${e.localizedMessage}")
                showError("No se pudo enviar la contraoferta. Verifica tu conexión e inténtalo nuevamente.")
            } finally {
                hideLoading()
            }
        }
    }
}
