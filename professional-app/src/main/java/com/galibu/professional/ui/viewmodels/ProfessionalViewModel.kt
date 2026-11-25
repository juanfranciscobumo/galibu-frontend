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

    /**
     * Tomar una solicitud abierta. El backend asigna el trabajo y lo pasa a
     * NEGOTIATING; desde ese momento todo se acuerda por el chat.
     */
    /**
     * Envía una postulación. Devuelve el id del job hijo (postulación) que
     * tiene su propio chat, no el id de la solicitud publicada.
     */
    fun applyToJob(jobId: String, onApplied: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Enviando solicitud...")
                val application = repository.applyToJob(jobId)
                repository.refreshJobDetails(application.id)
                hideLoading()
                onApplied(application.id)
            } catch (e: Exception) {
                hideLoading()
                Log.e("ProfessionalViewModel", "Error al enviar la postulación: ${e.localizedMessage}")
                handleProError(
                    e.localizedMessage ?: "",
                    "No se pudo enviar la solicitud. Puede que la publicación ya esté cerrada."
                )
            }
        }
    }

    /** Compatibilidad con llamadas antiguas. */
    fun acceptJob(jobId: String, onComplete: () -> Unit = {}) {
        applyToJob(jobId) { onComplete() }
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

    override fun startService(jobId: String, verificationCode: String, onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                showLoading("Iniciando servicio...")
                kotlinx.coroutines.delay(800)
                repository.startService(jobId, verificationCode)
                repository.refreshJobDetails(jobId)
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                showError("Código de verificación inválido o no se pudo iniciar el servicio.")
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
                repository.refreshJobDetails(jobId)
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                showError("No se pudo finalizar el servicio. Verifica tu conexión e inténtalo nuevamente.")
                Log.e("ProfessionalViewModel", "Error al finalizar servicio: ${e.localizedMessage}")
            }
        }
    }

    override fun cancelJob(jobId: String, reason: String, onComplete: () -> Unit) {
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
    
}
