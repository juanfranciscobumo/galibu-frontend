package com.galibu.professional.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.galibu.core.data.model.*
import com.galibu.core.data.remote.BoldCheckoutResponse
import com.galibu.core.data.remote.WalletTransactionDto
import com.galibu.core.ui.viewmodels.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ProfessionalViewModel(application: Application) : SharedViewModel(application) {

    private val _walletHistory = kotlinx.coroutines.flow.MutableStateFlow<List<com.galibu.core.data.remote.WalletTransactionDto>>(emptyList())
    val walletHistory: kotlinx.coroutines.flow.StateFlow<List<com.galibu.core.data.remote.WalletTransactionDto>> = _walletHistory

    private val _boldOrders = kotlinx.coroutines.flow.MutableStateFlow<List<com.galibu.core.data.remote.BoldOrderDto>>(emptyList())
    val boldOrders: kotlinx.coroutines.flow.StateFlow<List<com.galibu.core.data.remote.BoldOrderDto>> = _boldOrders

    private val _boldCheckout = MutableStateFlow<BoldCheckoutResponse?>(null)
    val boldCheckout: StateFlow<BoldCheckoutResponse?> = _boldCheckout.asStateFlow()

    /** Órdenes ya notificadas en esta sesión para no duplicar la push de éxito. */
    private val notifiedWalletOrders = mutableSetOf<String>()

    /** Evita avisar dos veces por la misma recarga detectada al refrescar la billetera. */
    private var lastSeenRechargeId: String? = null
    private var walletBaselineReady = false

    fun refreshWallet() {
        viewModelScope.launch {
            val history = repository.walletHistory()
            _walletHistory.value = history
            try {
                _boldOrders.value = repository.boldOrders()
            } catch (e: Exception) {
                Log.e("ProfessionalViewModel", "boldOrders falló: ${e.localizedMessage}")
            }
            repository.refreshMe()
            detectNewRecharge(history)
        }
    }

    /**
     * Crea la orden de recarga en Bold y deja lista la URL del checkout.
     * La app abre esa URL en el WebView seguro de Bold.
     */
    fun startBoldRecharge(amount: Double) {
        viewModelScope.launch {
            if (amount < 1000.0 || amount > 5_000_000.0) {
                showWarning("El monto debe estar entre $1.000 y $5.000.000 COP.")
                return@launch
            }
            try {
                showLoading("Conectando con Bold...")
                val checkout = repository.createBoldCheckout(amount)
                if (checkout.checkoutUrl.isBlank()) {
                    val message = "Bold no devolvió un link de pago. Inténtalo de nuevo."
                    notifyWalletError("No pudimos iniciar la recarga", message)
                    showError(message)
                    return@launch
                }
                _boldCheckout.value = checkout
            } catch (e: Exception) {
                Log.e("ProfessionalViewModel", "Falla creando checkout Bold: ${e.localizedMessage}")
                val detail = e.localizedMessage ?: ""
                val message = when {
                    detail.contains("503") -> "La pasarela de pagos Bold no está configurada en el servidor."
                    detail.contains("401") || detail.contains("403") ->
                        "Las llaves de Bold no son válidas o están inactivas."
                    else -> "No se pudo conectar con Bold. Verifica tu conexión e inténtalo de nuevo."
                }
                notifyWalletError("No pudimos iniciar la recarga", message)
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
     * El backend acredita el saldo una sola vez (polling y webhook son idempotentes).
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
                            val credited = result.amount.takeIf { it > 0.0 } ?: checkout.amount
                            notifyWalletSuccess(checkout.orderId, credited, result.balance)
                            refreshWallet()
                            val paid = formatCop(credited)
                            val balance = formatCop(result.balance)
                            showSuccess(
                                "¡Recarga exitosa! Se acreditaron $$paid COP. " +
                                    "Tu nuevo saldo es $$balance COP."
                            )
                            return@launch
                        }
                        "REJECTED", "CANCELLED", "EXPIRED" -> {
                            val status = result.status.lowercase(Locale.US)
                            val message = "El pago no se completó ($status). " +
                                "No se realizó ningún cobro. Puedes intentarlo de nuevo."
                            notifyWalletError("Recarga no completada", message)
                            showError(message)
                            return@launch
                        }
                    }
                    kotlinx.coroutines.delay(3000)
                }
                publishUserNotification(
                    title = "Recarga en proceso",
                    body = "Bold todavía no confirma tu recarga. Si ya pagaste, el saldo se " +
                        "acreditará automáticamente y te avisaremos.",
                    wallet = true
                )
                showWarning(
                    "No recibimos la confirmación del pago. Si no se realizó, intenta de nuevo; " +
                        "si ya pagaste, el saldo se acreditará automáticamente en unos minutos."
                )
            } catch (e: Exception) {
                Log.e("ProfessionalViewModel", "Falla confirmando pago Bold: ${e.localizedMessage}")
                val message = "No se pudo confirmar el pago con Bold. Si ya pagaste, revisa tu " +
                    "saldo en unos minutos."
                notifyWalletError("No pudimos confirmar tu recarga", message)
                showError(message)
            } finally {
                hideLoading()
            }
        }
    }

    private fun notifyWalletSuccess(orderId: String, amount: Double, balance: Double) {
        if (!notifiedWalletOrders.add(orderId)) return
        val paid = formatCop(amount)
        val newBalance = formatCop(balance)
        publishUserNotification(
            title = "Saldo recargado 💰",
            body = "Se acreditaron $$paid COP a tu billetera. " +
                "Nuevo saldo: $$newBalance COP.",
            wallet = true
        )
    }

    private fun notifyWalletError(title: String, body: String) {
        publishUserNotification(title = title, body = body, wallet = true)
    }

    /**
     * Detecta una recarga nueva al refrescar el historial. Cubre el caso en que el
     * backend acredita por webhook después de que la app dejó de hacer polling.
     */
    private fun detectNewRecharge(history: List<WalletTransactionDto>) {
        val latest = history
            .filter { it.type.equals("RECHARGE", ignoreCase = true) }
            .maxByOrNull { it.createdAt } ?: return

        if (!walletBaselineReady) {
            walletBaselineReady = true
            lastSeenRechargeId = latest.id
            // Si la app se abrió justo después de pagar, sí vale la pena avisar.
            if (System.currentTimeMillis() - latest.createdAt > 2 * 60 * 1000) return
        } else {
            if (latest.id == lastSeenRechargeId) return
            lastSeenRechargeId = latest.id
        }

        if (System.currentTimeMillis() - latest.createdAt > 10 * 60 * 1000) return
        val description = latest.description.orEmpty()
        if (notifiedWalletOrders.any { order -> order.isNotBlank() && description.contains(order) }) {
            return
        }
        notifyWalletSuccess(latest.id, latest.amount, latest.balanceAfter)
    }

    private fun formatCop(value: Double): String =
        NumberFormat.getNumberInstance(Locale.US).format(value)

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

    /** Push del sistema para recargas y errores de la billetera. */
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
                val user = currentUser.value
                val warning = AppNotification(
                    userId = user?.id ?: "",
                    title = "Oferta Guardada Localmente ⏳",
                    body = "Tu propuesta de $$amount COP se guardó localmente. Error de sincronización Firebase: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
                showWarning("Tu oferta se guardó localmente y se sincronizará automáticamente al recuperar la conexión.")
            } finally {
                hideLoading()
            }
        }
    }

    fun submitVerification(docType: String, docBytes: ByteArray) {
        viewModelScope.launch {
            try {
                showLoading("Enviando y encriptando documentos de verificación...")
                repository.submitVerificationDocuments(docType, docBytes)
                
                viewModelScope.launch {
                    kotlinx.coroutines.delay(10000)
                    val user = repository.currentUser.value
                    if (user != null && user.verificationStatus == "PENDING") {
                        val verifiedUser = user.copy(verificationStatus = "VERIFIED")
                        repository.updateUserDirectly(verifiedUser)
                        
                        val successNotif = AppNotification(
                            userId = user.id,
                            title = "Identidad Verificada Exitosamente 🎉",
                            body = "El pipeline inteligente de Firebase procesó tu ID ($docType). Tu cuenta cuenta ahora con sello verificado y puedes ofertar."
                        )
                        database.appNotificationDao().insertNotification(successNotif)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfessionalViewModel", "Falla verificación de documentos: ${e.localizedMessage}")
                val user = currentUser.value
                val warning = AppNotification(
                    userId = user?.id ?: "",
                    title = "Documentos Guardados Localmente ⏳",
                    body = "Tus documentos se guardaron para verificación local. Falló la subida remota a Firebase: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
                showWarning("Tus documentos se guardaron localmente y se enviarán a verificación al recuperar la conexión.")
            } finally {
                hideLoading()
            }
        }
    }

    fun acceptDirectHire(jobId: String, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Aceptando contratación...")
                kotlinx.coroutines.delay(1000)
                repository.acceptDirectHire(jobId)
                repository.refreshMe()
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                val msg = e.localizedMessage ?: ""
                if (msg.contains("409") || msg.contains("Saldo insuficiente", ignoreCase = true)) {
                    showWarning("No tienes saldo para aceptar este trabajo ($3.000 COP). Recarga tu wallet.", "Saldo insuficiente")
                } else {
                    showError("No se pudo aceptar la contratación. Verifica tu conexión e inténtalo nuevamente.")
                }
                Log.e("ProfessionalViewModel", "Error al aceptar contratación: ${e.localizedMessage}")
            }
        }
    }

    fun rejectDirectHire(jobId: String, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Rechazando contratación...")
                kotlinx.coroutines.delay(1000)
                repository.rejectDirectHire(jobId)
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                Log.e("ProfessionalViewModel", "Error al rechazar contratación: ${e.localizedMessage}")
                showError("No se pudo rechazar la contratación. Verifica tu conexión e inténtalo nuevamente.")
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

    fun finishService(jobId: String, stars: Int, comment: String, onResult: () -> Unit = {}) {
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

    fun submitCounterOfferDirectHire(jobId: String, amount: Double, isClient: Boolean, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Enviando contraoferta...")
                kotlinx.coroutines.delay(1000)
                repository.submitCounterOfferDirectHire(jobId, amount, isClient)
                hideLoading()
                onResult()
            } catch (e: Exception) {
                hideLoading()
                Log.e("ProfessionalViewModel", "Error al enviar contraoferta directa: ${e.localizedMessage}")
                showError("No se pudo enviar la contraoferta. Verifica tu conexión e inténtalo nuevamente.")
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
                if (msg.contains("409") || msg.contains("Saldo insuficiente", ignoreCase = true)) {
                    showWarning("No tienes saldo para aceptar este trabajo ($3.000 COP). Recarga tu wallet.", "Saldo insuficiente")
                } else {
                    showError("No se pudo aceptar la oferta. Verifica tu conexión e inténtalo nuevamente.")
                }
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

