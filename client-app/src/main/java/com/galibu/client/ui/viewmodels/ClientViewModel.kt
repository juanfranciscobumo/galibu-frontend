package com.galibu.client.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.galibu.core.data.model.*
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
        // Push de ofertas eliminadas; se ignora.
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

    /**
     * Finaliza el servicio sin calificar. La calificación es opcional: si el
     * cliente cierra la hoja de estrellas, el trabajo se cierra igual y puede
     * calificar más tarde desde el historial.
     */
    fun finishServiceWithoutRating(jobId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Finalizando servicio...")
                repository.finishService(jobId)
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

