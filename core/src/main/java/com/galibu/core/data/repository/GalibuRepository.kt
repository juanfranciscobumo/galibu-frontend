package com.galibu.core.data.repository

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import java.util.Locale
import com.galibu.core.data.local.*
import com.galibu.core.data.model.*
import com.galibu.core.data.firebase.FirebaseService
import com.galibu.core.data.remote.AddRoleDto
import com.galibu.core.data.remote.AmountDto
import com.galibu.core.data.remote.ApiService
import com.galibu.core.data.remote.BoldCheckoutResponse
import com.galibu.core.data.remote.BoldOrderStatusResponse
import com.galibu.core.data.remote.CreateJobDto
import com.galibu.core.data.remote.GoogleLoginRequestDto
import com.galibu.core.data.remote.LoginRequestDto
import com.galibu.core.data.remote.DeviceTokenRequestDto
import com.galibu.core.data.remote.PasswordChangeDto
import com.galibu.core.data.remote.RefreshRequestDto
import com.galibu.core.data.remote.RegisterRequestDto
import com.galibu.core.data.remote.SubmitBidDto
import com.galibu.core.data.remote.SubscriptionCheckoutRequest
import com.galibu.core.data.remote.SubscriptionDto
import com.galibu.core.data.remote.TokenStore
import com.galibu.core.data.remote.UpdateProfileDto
import com.galibu.core.data.remote.WalletTransactionDto
import com.galibu.core.data.remote.RequestTrace
import com.galibu.core.data.telemetry.AppTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import com.galibu.core.data.firebase.awaitTask
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class GalibuRepository(
    private val userProfileDao: UserProfileDao,
    private val jobRequestDao: JobRequestDao,
    private val bidDao: BidDao,
    private val chatMessageDao: ChatMessageDao,
    private val notificationDao: AppNotificationDao,
    private val firebaseService: FirebaseService,
    private val apiService: ApiService,
    private val tokenStore: TokenStore
) {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser = _currentUser.asStateFlow()

    suspend fun insertBidDirectly(bid: Bid) = withContext(Dispatchers.IO) {
        bidDao.insertBid(bid)
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Track job IDs that were recently modified locally to prevent sync race conditions
    private val locallyModifiedJobIds = mutableSetOf<String>()

    /**
     * Las apps lo asignan para mostrar push del sistema ante eventos del ciclo
     * de vida (aceptar/iniciar/finalizar/calificar). El repositorio no tiene
     * Context: solo inserta la notificación in-app e invoca este callback.
     */
    var onEventNotification: ((AppNotification) -> Unit)? = null

    /**
     * Compara el job local con el que trae el backend y devuelve (título, cuerpo)
     * si el usuario actual debe ser notificado. Solo dispara con cambios reales
     * (el autor del cambio ya lo tiene en local y no se auto-notifica).
     */
    private fun detectLifecycleEvent(
        old: JobRequest,
        new: JobRequest,
        currentUserId: String
    ): Pair<String, String>? {
        val isPro = new.professionalId == currentUserId
        val isClient = new.clientId == currentUserId
        if (!isPro && !isClient) return null
        val title = new.title.takeIf { it.isNotBlank() }
            ?: new.category.takeIf { it.isNotBlank() } ?: "tu solicitud"
        val proName = new.professionalName?.takeIf { it.isNotBlank() } ?: "El profesional"
        val clientName = new.clientName.takeIf { it.isNotBlank() } ?: "El cliente"

        if (!old.status.equals(new.status, ignoreCase = true)) {
            return when {
                isPro && new.status == "ACCEPTED" ->
                    "¡Oferta aceptada! 🎉" to
                        "$clientName aceptó tu oferta en '$title'. Coordina el inicio por chat."
                isClient && new.status == "IN_PROGRESS" ->
                    "¡Servicio iniciado! 🚀" to
                        "$proName inició el trabajo de '$title'. Puedes chatear para coordinar."
                isClient && new.status == "COMPLETED" ->
                    "¡Servicio finalizado! ⭐" to
                        "$proName terminó '$title'. Califícalo para completar el cierre."
                isPro && new.status == "COMPLETED" ->
                    "¡Servicio completado! 🏆" to
                        "El servicio '$title' quedó completado. Revisa tu calificación y pago."
                else -> null
            }
        }
        // Sin cambio de estado: espejos de calificación recién llegados.
        if (isPro && old.clientRatingOfPro == null && new.clientRatingOfPro != null) {
            val stars = new.clientRatingOfPro
            return if (old.status == "COMPLETED") {
                "¡Te calificaron! ⭐" to
                    "$clientName te calificó con $stars estrellas en '$title'."
            } else {
                "¡El cliente finalizó! 📝" to
                    "$clientName calificó con $stars estrellas '$title'. Finaliza el servicio y califica al cliente."
            }
        }
        if (isClient && old.proRatingOfClient == null && new.proRatingOfClient != null) {
            return "¡Te calificaron! ⭐" to
                "$proName te calificó con ${new.proRatingOfClient} estrellas en '$title'."
        }
        return null
    }

    // No network calls in init: the app triggers refreshAll() explicitly after startup,
    // which keeps unit tests (JVM) free of side effects.

    /**
     * Pulls the professionals directory from the backend and merges it into Room,
     * applying the same stale-cleanup rules the old real-time listener had.
     */
    suspend fun refreshProfessionals() = withContext(Dispatchers.IO) {
        try {
            val pros = apiService.professionals()
            val currentUserId = _currentUser.value?.id ?: ""
            val proIds = pros.map { it.id }.filter { it.isNotEmpty() }
            Log.d("GalibuRepo", "Professionals refresh: ${pros.size} pros from backend, currentUserId=$currentUserId")
            pros.forEach { pro ->
                userProfileDao.insertUser(pro)
            }
            if (proIds.isEmpty()) {
                Log.w("GalibuRepo", "WARNING: Server returned 0 professionals! Deleting all from Room.")
                userProfileDao.deleteAllProfessionalsExcept(currentUserId)
            } else {
                userProfileDao.deleteProfessionalsNotIn(proIds, currentUserId)
            }
        } catch (e: Exception) {
            Log.e("GalibuRepo", "Error refreshing professionals: ${e.localizedMessage}")
            throw e
        }
    }

    private fun setCurrentUser(user: UserProfile?) {
        _currentUser.value = user
        refreshJobsForCurrentUser(user)
    }

    private fun refreshJobsForCurrentUser(user: UserProfile?) {
        if (user == null || user.id.isBlank()) {
            return
        }
        repositoryScope.launch(RequestTrace.currentContext()) {
            try {
                refreshJobs()
            } catch (e: Exception) {
                Log.e("GalibuRepository", "Error en refresh inicial de jobs: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Restores the session from stored JWT tokens (survives app kill).
     * Only an explicit [logout] clears tokens. Returns true when a user session is active.
     */
    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        if (!tokenStore.hasTokens()) return@withContext false
        try {
            val me = apiService.getMe()
            userProfileDao.insertUser(me)
            setCurrentUser(me)
            true
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error restaurando sesión: ${e.localizedMessage}")
            AppTelemetry.error("db", "No se pudo restaurar la sesión", e)
            false
        }
    }

    /**
     * Promuelve explícitamente la cuenta a BOTH activando la capacidad que
     * falta (CLIENT o PROFESSIONAL). Devuelve el perfil actualizado o null si
     * falló. Es la vía que usa el upgrade desde Perfil: no crea cuentas nuevas
     * ni toca saldo; el backend otorga las 3 cortesías una sola vez al pasar
     * de CLIENT a off--r el registro.
     */
    suspend fun addRole(role: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val expanded = apiService.addRole(AddRoleDto(role))
            val profile = try { apiService.getMe() } catch (e: Exception) {
                Log.e("GalibuRepository", "No se pudo releer el perfil tras activar el rol: ${e.localizedMessage}")
                expanded
            }
            userProfileDao.insertUser(profile)
            setCurrentUser(profile)
            profile
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo activar el rol $role: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Garantiza que el usuario actual tenga el rol que exige la app en la que
     * corre la sesión. Si su rol es distinto (p. ej. CLIENT en la app de
     * profesionales) y no es BOTH, se expande a BOTH vía backend.
     * Devuelve true si el rol quedó alineado (o ya lo estaba).
     */
    suspend fun ensureRole(intendedRole: String): Boolean = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext false
        if (user.role.equals(intendedRole, ignoreCase = true) || user.role.equals("BOTH", ignoreCase = true)) {
            return@withContext true
        }
        addRole(intendedRole) != null
    }

    /** Timestamp de la última ejecución de refreshJobs (para throttle). */
    private var lastRefreshJobsMs = 0L
    private val REFRESH_JOBS_THROTTLE_MS = 60_000L // respaldo del push FCM; evita saturar la API

    /**
     * Pulls jobs from the backend (mine + marketplace when the role allows it)
     * and merges them into Room with the usual stale-cleanup.
     * Throttled: no ejecuta más de una vez cada 30 segundos para reducir llamadas a la API.
     */
    suspend fun refreshJobs() {
        val user = _currentUser.value ?: return
        val now = System.currentTimeMillis()
        if (lastRefreshJobsMs > 0L && now - lastRefreshJobsMs < REFRESH_JOBS_THROTTLE_MS) {
            Log.d("GalibuRepository", "refreshJobs throttled (última ejecución hace ${now - lastRefreshJobsMs}ms)")
            return
        }
        lastRefreshJobsMs = now
        val includeOpen = user.role.equals("PROFESSIONAL", ignoreCase = true) || user.role.equals("BOTH", ignoreCase = true)
        val jobs = apiService.jobs(
            clientId = user.id,
            professionalId = user.id,
            status = if (includeOpen) "OPEN" else null
        )
        syncJobsSnapshot(jobs, isFromCache = false, user.id)
    }

    /**
     * Fuerza un refresh de jobs ignorando el throttle (para pull-to-refresh manual).
     */
    suspend fun forceRefreshJobs() {
        lastRefreshJobsMs = 0L
        refreshJobs()
    }

    /**
     * Pulls bids from the backend into Room: own bids for professionals,
     * plus every bid of the given job (the backend already scopes by caller).
     */
    suspend fun refreshBids() {
        val user = _currentUser.value ?: return
        if (!user.role.equals("PROFESSIONAL", ignoreCase = true) && !user.role.equals("BOTH", ignoreCase = true)) return
        try {
            apiService.myBids().forEach { bidDao.insertBid(it) }
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error refrescando bids: ${e.localizedMessage}")
            throw e
        }
    }

    suspend fun refreshBidsForJob(jobId: String) {
        try {
            apiService.jobBids(jobId).forEach { bidDao.insertBid(it) }
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error refrescando bids del job $jobId: ${e.localizedMessage}")
        }
    }

    suspend fun refreshJobDetails(jobId: String) {
        RequestTrace.withTransaction {
            try {
                val job = apiService.getJob(jobId)
                jobRequestDao.insertJob(job)
            } catch (e: Exception) {
                Log.e("GalibuRepository", "Error refrescando job $jobId: ${e.localizedMessage}")
            }
            refreshBidsForJob(jobId)
        }
    }

    /**
     * Trae el estado fresco del job (backend -> Room) y lo devuelve
     * para reflejarlo en pantalla sin salir de ella.
     */
    suspend fun getJobSnapshot(jobId: String): JobRequest? = withContext(Dispatchers.IO) {
        try {
            refreshJobDetails(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "getJobSnapshot refresh falló: ${e.localizedMessage}")
        }
        try {
            jobRequestDao.getJobByIdSync(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "getJobSnapshot lectura falló: ${e.localizedMessage}")
            null
        }
    }

    suspend fun refreshAll() {
        RequestTrace.withTransaction {
            try { refreshProfessionals() } catch (e: Exception) {
                Log.e("GalibuRepository", "refreshAll professionals falló: ${e.localizedMessage}")
            }
            try { forceRefreshJobs() } catch (e: Exception) {
                Log.e("GalibuRepository", "refreshAll jobs falló: ${e.localizedMessage}")
            }
            try { refreshBids() } catch (e: Exception) {
                Log.e("GalibuRepository", "refreshAll bids falló: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun syncJobsSnapshot(jobs: List<JobRequest>, isFromCache: Boolean, currentUserId: String) {
        try {
            val jobIds = jobs.map { it.id }.filter { it.isNotEmpty() }

            jobs.forEach { job ->
                try {
                    val existingLocal = try {
                        jobRequestDao.getJobByIdSync(job.id)
                    } catch (e: Exception) {
                        null
                    }
                    // Eventos del ciclo de vida (aceptar/iniciar/finalizar/calificar):
                    // el otro extremo solo los ve cuando el sync trae el cambio.
                    if (existingLocal != null && currentUserId.isNotEmpty()) {
                        detectLifecycleEvent(existingLocal, job, currentUserId)?.let { (title, body) ->
                            try {
                                val eventNotif = AppNotification(
                                    userId = currentUserId,
                                    title = title,
                                    body = body,
                                    timestamp = System.currentTimeMillis(),
                                    isRead = false,
                                    jobId = job.id
                                )
                                notificationDao.insertNotification(eventNotif)
                                onEventNotification?.invoke(eventNotif)
                            } catch (e: Exception) {
                                Log.e("GalibuRepository", "Error guardando notificación de evento: ${e.localizedMessage}")
                            }
                        }
                    }
                    if (job.professionalId == currentUserId && currentUserId.isNotEmpty()) {
                        if (existingLocal == null) {
                            val notification = AppNotification(
                                userId = currentUserId,
                                title = "¡Nueva propuesta de contratación! 🛠️",
                                body = "${job.clientName} te ha enviado una solicitud de servicio directo de ${job.category}.",
                                timestamp = System.currentTimeMillis(),
                                isRead = false
                            )
                            notificationDao.insertNotification(notification)
                        }
                    }

                    if (job.clientId == currentUserId && currentUserId.isNotEmpty()) {
                        // Bids for own jobs arrive via refreshBidsForJob (backend).
                    }

                    // Professionals: unassigned NEGOTIATING jobs are only visible to a
                    // professional if their bid is synced locally (jobIdsWithBids).
                    // Bids arrive via refreshBids()/refreshBidsForJob (backend).

                    jobRequestDao.insertJob(job)
                } catch (e: Exception) {
                    Log.e("GalibuRepository", "Error syncing/inserting single job ${job.id}: ${e.localizedMessage}")
                }
            }

            if (!isFromCache) {
                if (jobIds.isEmpty()) {
                    // Even if server says no jobs, keep locally-modified ones
                    val localIds = locallyModifiedJobIds.toList()
                    if (localIds.isNotEmpty()) {
                        localIds.forEach { id ->
                            val localJob = jobRequestDao.getJobByIdSync(id)
                            if (localJob != null) jobRequestDao.insertJob(localJob)
                        }
                    } else {
                        jobRequestDao.deleteAllJobs()
                    }
                } else {
                    // Merge server job IDs with locally-modified IDs to prevent race condition deletion
                    val safeIds = (jobIds + locallyModifiedJobIds.toList()).distinct()
                    jobRequestDao.deleteJobsNotIn(safeIds)
                    locallyModifiedJobIds.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("GalibuRepository", "General error in jobs background sync flow: ${e.localizedMessage}")
        }
    }

    private fun currentUserIdMismatch(currentId: String?, profileId: String): Boolean {
        return currentId == null || currentId != profileId
    }

    private fun UserProfile.toUpdateDto() = UpdateProfileDto(
        name = name,
        phone = phone,
        profilePhotoUrl = profilePhotoUrl,
        department = department,
        municipality = municipality,
        address = address,
        workCategory = workCategory,
        hourlyRate = hourlyRate,
        workPhotos = workPhotos,
        workDescription = workDescription
    )

    /**
     * Pushes profile edits to the backend (own profile only; the server rejects
     * anything else, which then stays Room-only like the old rules did).
     * Returns the server version, or the input when offline/failed.
     */
    private suspend fun pushProfileToBackend(profile: UserProfile): UserProfile {
        val currentId = _currentUser.value?.id
        if (currentUserIdMismatch(currentId, profile.id)) {
            try { userProfileDao.insertUser(profile) } catch (ignored: Exception) { }
            return profile
        }
        return try {
            val updated = apiService.updateMe(profile.toUpdateDto())
            userProfileDao.insertUser(updated)
            updated
        } catch (e: Exception) {
            Log.e("GalibuRepository", "pushProfileToBackend falló, se conserva local: ${e.localizedMessage}")
            try { userProfileDao.insertUser(profile) } catch (ignored: Exception) { }
            profile
        }
    }

    private fun refreshCurrentUserProfile() {
        repositoryScope.launch(RequestTrace.currentContext()) {
            try {
                val updatedProfile = apiService.getMe()
                userProfileDao.insertUser(updatedProfile)
                if (_currentUser.value?.id == updatedProfile.id) {
                    setCurrentUser(updatedProfile)
                }
            } catch (e: Exception) {
                Log.e("GalibuRepository", "Error refrescando perfil: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Sincroniza los mensajes de un chat desde el backend (fuente de verdad) hacia Room.
     * Devuelve null porque ya no usamos listeners de Firestore para el chat.
     */
    fun syncChatsForJob(jobId: String): com.google.firebase.firestore.ListenerRegistration? {
        repositoryScope.launch {
            try {
                val msgs = apiService.jobChats(jobId)
                msgs.forEach { chatMessageDao.insertMessage(it) }
            } catch (e: Exception) {
                Log.e("GalibuRepository", "Error sincronizando chat $jobId desde backend: ${e.localizedMessage}")
            }
        }
        return null
    }

    suspend fun refreshChatsForJob(jobId: String) = withContext(Dispatchers.IO) {
        try {
            val msgs = apiService.jobChats(jobId)
            msgs.forEach { chatMessageDao.insertMessage(it) }
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error refrescando chat $jobId: ${e.localizedMessage}")
        }
    }

    private suspend fun seedInitialData() {
        // Disabled for real environment testing as requested
    }

    // --- Authentication & Profiles ---

    suspend fun loginWithGoogle(
        idToken: String? = null,
        email: String,
        name: String,
        avatarUrl: String?,
        role: String,
        customPhone: String? = null,
        customDepartment: String? = null,
        customMunicipality: String? = null,
        customAddress: String? = null,
        customCategory: String? = null,
        customWorkPhotos: List<String> = emptyList(),
        customHourlyRate: Double? = null,
        context: Context? = null
    ): String? = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            loginWithGoogleInternal(
                idToken = idToken,
                email = email,
                name = name,
                avatarUrl = avatarUrl,
                role = role,
                customPhone = customPhone,
                customDepartment = customDepartment,
                customMunicipality = customMunicipality,
                customAddress = customAddress,
                customCategory = customCategory,
                customWorkPhotos = customWorkPhotos,
                customHourlyRate = customHourlyRate,
                context = context
            )
        }
    }

    private suspend fun loginWithGoogleInternal(
        idToken: String?,
        email: String,
        name: String,
        avatarUrl: String?,
        role: String,
        customPhone: String?,
        customDepartment: String?,
        customMunicipality: String?,
        customAddress: String?,
        customCategory: String?,
        customWorkPhotos: List<String>,
        customHourlyRate: Double?,
        context: Context?
    ): String? {
        // Login con Google: solo autentica cuentas existentes (mode=LOGIN).
        // Sin un ID token no hay sesión que establecer.
        if (idToken.isNullOrEmpty()) {
            Log.w("GalibuRepository", "loginWithGoogle sin ID token; se rechaza el ingreso para evitar sesión fantasma.")
            AppTelemetry.warn("auth", "Google login sin ID token")
            return "No se pudo obtener credenciales de Google. Intenta nuevamente."
        }
        val normalizedEmail = email.trim().lowercase()
        val auth = try {
            apiService.google(GoogleLoginRequestDto(idToken, role, "LOGIN"))
        } catch (e: HttpException) {
            Log.e("GalibuRepository", "Google login rechazado (${e.code()}): ${e.localizedMessage}")
            AppTelemetry.warn("auth", "Google login rechazado (${e.code()})")
            return when {
                e.code() == 404 -> "No existe una cuenta con este correo. Regístrate primero."
                e.code() == 409 -> "El correo ya está registrado. Inicia sesión con tu contraseña."
                else -> backendErrorMessage(e)
                    ?: "No se pudieron verificar las credenciales con Google."
            }
        } catch (e: IOException) {
            Log.e("GalibuRepository", "Google login sin conexión: ${e.localizedMessage}")
            AppTelemetry.error("auth", "Google login sin conexión", e)
            return "Sin conexión: no se pudo iniciar sesión con Google."
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Google login falló: ${e.localizedMessage}")
            AppTelemetry.error("auth", "Google login falló", e)
            return "No se pudo iniciar sesión con Google. Inténtalo nuevamente."
        }
        tokenStore.saveTokens(auth.accessToken, auth.refreshToken)
        val backendUser = auth.user
            ?: return "No se pudo iniciar sesión. Inténtalo nuevamente."
        // Existing account material: the backend owns the role. The frontend
        // never expands an existing account silently — activation of the other
        // capability is explicit from the Profile menu.
        var target: UserProfile? = backendUser

        // Upload work photos if we have a context
        var workPhotosCsv: String? = null
        if (role == "PROFESSIONAL" && customWorkPhotos.isNotEmpty() && context != null) {
            val uploadedWorkPhotos = mutableListOf<String>()
            customWorkPhotos.forEachIndexed { index, uriStr ->
                if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                    try {
                        val uri = uriStr.toUri()
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                                if (bytes != null) {
                                    val url = firebaseService.uploadWorkPhoto(backendUser.id, "google_work_$index", bytes)
                                    uploadedWorkPhotos.add(url)
                                }
                    } catch (e: Exception) {
                        Log.e("GalibuRepository", "Error uploading google work photo $index: ${e.localizedMessage}")
                    }
                } else {
                    uploadedWorkPhotos.add(uriStr)
                }
            }
            workPhotosCsv = if (uploadedWorkPhotos.isNotEmpty()) uploadedWorkPhotos.joinToString("|") else null
        }

        if (target != null) {
            // El backend ya resolvió la expansión de rol; se usa el rol real.
            val targetRole = target.role
            val finalPhotoUrl = if ((targetRole == "CLIENT" || targetRole == "BOTH") && context != null && !avatarUrl.isNullOrEmpty()) {
                val defaultPhoto = if (targetRole == "CLIENT") {
                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop"
                } else {
                    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150&h=150&fit=crop"
                }
                processProfilePhotoToBase64(context, avatarUrl, defaultPhoto)
            } else {
                avatarUrl ?: target.profilePhotoUrl
            }
            val updatedUser = target.copy(
                id = target.id,
                name = if (target.name.isBlank() || target.name == "María García" || target.name == "Cliente Galibu") name else target.name,
                profilePhotoUrl = finalPhotoUrl,
                phone = customPhone ?: target.phone,
                department = customDepartment ?: target.department,
                municipality = customMunicipality ?: target.municipality,
                address = customAddress ?: target.address,
                role = targetRole,
                workCategory = customCategory ?: target.workCategory,
                workPhotos = workPhotosCsv ?: target.workPhotos,
                hourlyRate = customHourlyRate ?: target.hourlyRate
            )
            userProfileDao.insertUser(updatedUser)
            val synced = pushProfileToBackend(updatedUser)
            userProfileDao.insertUser(synced)
            setCurrentUser(synced)
            refreshCurrentUserProfile()
            AppTelemetry.event("auth.google.success", mapOf("role" to targetRole))
            return null
        }
        Log.w("GalibuRepository", "Inalcanzable: el backend no retornó usuario en loginWithGoogle.")
        return "No se pudo iniciar sesión. Inténtalo nuevamente."
    }

    suspend fun login(email: String, password: String, role: String): String? = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            val normalizedEmail = email.trim().lowercase()
            val auth = try {
                apiService.login(LoginRequestDto(normalizedEmail, password))
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    AppTelemetry.warn("auth", "Login rechazado: credenciales inválidas")
                    return@withTransaction "Correo o contraseña incorrectos. Por favor, verifica tus datos de ingreso."
                }
                Log.e("GalibuRepository", "Login rechazado (${e.code()}): ${e.localizedMessage}")
                AppTelemetry.warn("auth", "Login rechazado (${e.code()})")
                return@withTransaction "No se pudo iniciar sesión. Inténtalo nuevamente."
            } catch (e: IOException) {
                Log.e("GalibuRepository", "Login sin conexión: ${e.localizedMessage}")
                AppTelemetry.error("auth", "Login sin conexión", e)
                return@withTransaction "Error de red: No pudimos conectar con el servidor. Verifica tu internet e inténtalo nuevamente."
            } catch (e: Exception) {
                Log.e("GalibuRepository", "Login falló: ${e.localizedMessage}")
                AppTelemetry.error("auth", "Login falló", e)
                return@withTransaction "No se pudo iniciar sesión. Inténtalo nuevamente."
            }
            tokenStore.saveTokens(auth.accessToken, auth.refreshToken)
            val profile = auth.user
            if (profile == null) {
                tokenStore.clear()
                AppTelemetry.warn("auth", "Login sin perfil en la respuesta")
                return@withTransaction "No se pudo iniciar sesión. Inténtalo nuevamente."
            }
            // El backend define la capacidad; la app nunca expande en silencio
            // (el landing de login pasa CLIENT). La otra capacidad se activa
            // explícitamente desde Perfil.
            userProfileDao.insertUser(profile)
            setCurrentUser(profile)
            refreshCurrentUserProfile()
            AppTelemetry.event("auth.login.success", mapOf("role" to profile.role))
            return@withTransaction null
        }
    }

    suspend fun registerUser(context: Context, name: String, email: String, phone: String, role: String, category: String? = null, department: String? = "Bogotá D.C.", municipality: String? = "Bogotá", profilePhotoUrl: String? = null, address: String? = "", workPhotos: List<String> = emptyList(), hourlyRate: Double? = null, acceptedTerms: Boolean = false, googleIdToken: String? = null) {
        registerUser(context, name, email, phone, "", role, category, department, municipality, profilePhotoUrl, address, workPhotos, hourlyRate, acceptedTerms, googleIdToken)
    }

    suspend fun registerUser(context: Context, name: String, email: String, phone: String, password: String, role: String, category: String? = null, department: String? = "Bogotá D.C.", municipality: String? = "Bogotá", profilePhotoUrl: String? = null, address: String? = "", workPhotos: List<String> = emptyList(), hourlyRate: Double? = null, acceptedTerms: Boolean = false, googleIdToken: String? = null) = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
        val normalizedEmail = email.trim().lowercase()
        // 1. Account on the backend (source of ids + JWT tokens).
        val base: UserProfile = try {
            if (!googleIdToken.isNullOrBlank()) {
                val resp = apiService.google(GoogleLoginRequestDto(googleIdToken, role, "REGISTER"))
                tokenStore.saveTokens(resp.accessToken, resp.refreshToken)
                resp.user ?: throw IllegalStateException("El servidor no retornó el perfil.")
            } else if (password.isNotBlank()) {
                try {
                    val resp = apiService.register(
                        RegisterRequestDto(name, normalizedEmail, phone, password, role, department, municipality, address)
                    )
                    tokenStore.saveTokens(resp.accessToken, resp.refreshToken)
                    resp.user ?: throw IllegalStateException("El servidor no retornó el perfil.")
                } catch (e: HttpException) {
                    if (e.code() != 409) throw e
                    // Email taken: log in to decide between "already registered" and role expansion.
                    val loginResp = try {
                        apiService.login(LoginRequestDto(normalizedEmail, password))
                    } catch (le: Exception) {
                        throw IllegalStateException("EMAIL_EXISTS")
                    }
                    if (loginResp.user == null) {
                        throw IllegalStateException("EMAIL_EXISTS")
                    }
                    // Cuenta existente: el backend conserva su rol. No se guardan
                    // tokens ni hay expansión silenciosa a BOTH; la otra capacidad
                    // se activa desde Perfil. El usuario inicia sesión aparte.
                    throw IllegalStateException("EMAIL_EXISTS")
                }
            } else {
                throw IllegalStateException("Se requiere contraseña o cuenta de Google para registrarse.")
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: HttpException) {
            Log.e("GalibuRepository", "Registro rechazado (${e.code()}): ${e.localizedMessage}")
            throw IllegalStateException(friendlyRegisterError(e))
        } catch (e: IOException) {
            Log.e("GalibuRepository", "Registro sin conexión: ${e.localizedMessage}")
            throw IllegalStateException("Sin conexión: no se pudo crear la cuenta.")
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Registro falló: ${e.localizedMessage}")
            throw IllegalStateException(e.localizedMessage ?: "No se pudo crear la cuenta.")
        }
        val finalId = base.id
        setCurrentUser(base)
        userProfileDao.insertUser(base)

        val defaultPhoto = if (role == "CLIENT") {
            "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop"
        } else {
            "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150&h=150&fit=crop"
        }

        var uploadedPhotoUrl = profilePhotoUrl
        if (uploadedPhotoUrl.isNullOrEmpty()) {
            uploadedPhotoUrl = defaultPhoto
        }

        if (role == "CLIENT") {
            uploadedPhotoUrl = processProfilePhotoToBase64(context, uploadedPhotoUrl, defaultPhoto)
        } else {
            if (!uploadedPhotoUrl.isNullOrEmpty() && uploadedPhotoUrl.startsWith("http")) {
                val base64 = com.galibu.core.ui.utils.ImageUtils.downloadUrlToBase64(uploadedPhotoUrl)
                if (base64 != null) {
                    uploadedPhotoUrl = base64
                }
            } else if (!uploadedPhotoUrl.isNullOrEmpty() && !uploadedPhotoUrl.startsWith("data:image")) {
                try {
                    val bytes = if (uploadedPhotoUrl.startsWith("content://") || uploadedPhotoUrl.startsWith("file://")) {
                        val uri = uploadedPhotoUrl.toUri()
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } else {
                        val file = java.io.File(uploadedPhotoUrl)
                        if (file.exists()) file.readBytes() else null
                    }
                    if (bytes != null) {
                        uploadedPhotoUrl = firebaseService.uploadProfilePhoto(finalId, bytes)
                    }
                } catch (e: Exception) {
                    Log.e("GalibuRepository", "Error uploading photo with final authorized UID: ${e.localizedMessage}")
                }
            }
        }

        // Upload work photos
        val uploadedWorkPhotos = mutableListOf<String>()
        workPhotos.forEachIndexed { index, uriStr ->
            if (uriStr.isNotBlank() && !uriStr.startsWith("http") && !uriStr.startsWith("data:image")) {
                try {
                    val bytes = if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                            val uri = uriStr.toUri()
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } else {
                        val file = java.io.File(uriStr)
                        if (file.exists()) file.readBytes() else null
                    }
                    if (bytes != null) {
                        val url = firebaseService.uploadWorkPhoto(finalId, "work_$index", bytes)
                        uploadedWorkPhotos.add(url)
                    }
                } catch (e: Exception) {
                    Log.e("GalibuRepository", "Error uploading work photo $index: ${e.localizedMessage}")
                }
            } else {
                uploadedWorkPhotos.add(uriStr)
            }
        }
        val workPhotosCsv = if (uploadedWorkPhotos.isNotEmpty()) uploadedWorkPhotos.joinToString("|") else null

        val withExtras = base.copy(
            name = if (name.isNotBlank()) name else base.name,
            phone = if (phone.isNotBlank()) phone else base.phone,
            profilePhotoUrl = uploadedPhotoUrl,
            department = department ?: base.department,
            municipality = municipality ?: base.municipality,
            address = address?.takeIf { it.isNotBlank() } ?: base.address,
            workCategory = category ?: base.workCategory,
            workPhotos = workPhotosCsv ?: base.workPhotos,
            hourlyRate = hourlyRate ?: base.hourlyRate,
            acceptedTerms = acceptedTerms
        )
        val synced = pushProfileToBackend(withExtras)
        // Cuenta creada por Google: nace CLIENT en el backend. Si el usuario
        // eligió ser profesional en el wizard, el upgrade se hace aquí de forma
        // explícita (nunca por el rol que mande el cliente en /auth/google).
        var finalUser = synced
        if (base.authProvider == "GOOGLE" && base.role == "CLIENT" && role != "CLIENT") {
            finalUser = try {
                apiService.addRole(AddRoleDto("BOTH"))
            } catch (e: Exception) {
                Log.e("GalibuRepository", "No se pudo activar la capacidad profesional: ${e.localizedMessage}")
                synced
            }
        }
        // Add welcome notification for the newly registered user
        try {
            val welcomeNotification = AppNotification(
                userId = synced.id,
                title = "¡Te damos la bienvenida a Galibu!",
                body = "Gracias por registrarte de forma segura en Galibu. Aquí puedes solicitar servicios técnicos de confianza, recibir cotizaciones transparentes y realizar pagos ágiles con soporte garantizado.",
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            notificationDao.insertNotification(welcomeNotification)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error al guardar notificación de bienvenida: ${e.localizedMessage}")
        }
        setCurrentUser(finalUser)
        AppTelemetry.event("auth.register.success", mapOf("role" to role))
        refreshCurrentUserProfile()
        }
    }

    suspend fun updateProfile(name: String, phone: String, workCategory: String?, profilePhotoUrl: String?, department: String? = "Bogotá D.C.", municipality: String? = "Bogotá", address: String? = null, hourlyRate: Double? = null, workPhotos: String? = null, workDescription: String? = null) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val updated = user.copy(
            name = name,
            phone = phone,
            workCategory = workCategory,
            profilePhotoUrl = profilePhotoUrl,
            department = department,
            municipality = municipality,
            address = address ?: user.address,
            hourlyRate = hourlyRate,
            workPhotos = workPhotos ?: user.workPhotos,
            workDescription = workDescription ?: user.workDescription
        )
        val synced = pushProfileToBackend(updated)
        setCurrentUser(synced)
        AppTelemetry.event("profile.update", mapOf("hasPhoto" to (!profilePhotoUrl.isNullOrBlank()).toString()))
    }

    suspend fun updateUserDirectly(profile: UserProfile) = withContext(Dispatchers.IO) {
        val synced = pushProfileToBackend(profile)
        setCurrentUser(synced)
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): String? = withContext(Dispatchers.IO) {
        if (currentPassword.isBlank()) {
            return@withContext "Ingresa tu contraseña actual."
        }
        if (newPassword.length < 8) {
            return@withContext "La nueva contraseña debe tener al menos 8 caracteres."
        }
        if (newPassword.length > 72) {
            return@withContext "La nueva contraseña no puede superar los 72 caracteres."
        }
        try {
            apiService.changePassword(PasswordChangeDto(currentPassword, newPassword))
            AppTelemetry.event("profile.password")
            null
        } catch (e: HttpException) {
            val serverMessage = backendErrorMessage(e)
            when (e.code()) {
                401 -> serverMessage ?: "La contraseña actual es incorrecta"
                in 400..499 -> serverMessage
                    ?: "No se pudo cambiar la contraseña. Revisa los datos e inténtalo de nuevo."
                else -> "No se pudo cambiar la contraseña en el servidor."
            }
        } catch (e: IOException) {
            "Sin conexión: no se pudo cambiar la contraseña."
        } catch (e: Exception) {
            "No se pudo cambiar la contraseña: ${e.localizedMessage}"
        }
    }

    suspend fun registerDeviceToken(fcmToken: String, platform: String = "ANDROID") = withContext(Dispatchers.IO) {
        try {
            apiService.registerDeviceToken(DeviceTokenRequestDto(fcmToken, platform))
            AppTelemetry.event("device.token.registered")
        } catch (e: Exception) {
            Log.e("GalibuRepository", "registerDeviceToken falló: ${e.localizedMessage}")
        }
    }

    /** Extrae el campo `message` del JSON de error del backend, si viene. */
    private fun backendErrorMessage(e: HttpException): String? {
        val body = try {
            e.response()?.errorBody()?.string().orEmpty()
        } catch (ignored: Exception) {
            ""
        }
        val raw = Regex("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            .find(body)?.groupValues?.getOrNull(1) ?: return null
        return raw
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .ifBlank { null }
    }

    suspend fun getUserById(id: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val remote = apiService.getUser(id)
            userProfileDao.insertUser(remote)
            remote
        } catch (e: Exception) {
            Log.e("GalibuRepository", "getUserById backend falló, usando Room: ${e.localizedMessage}")
            try {
                userProfileDao.getUserByIdSync(id)
            } catch (ignored: Exception) {
                null
            }
        }
    }

    suspend fun deleteAccount(): String? = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext null
        try {
            apiService.deleteMe()
        } catch (e: HttpException) {
            if (e.code() == 409) {
                return@withContext "No puedes eliminar tu cuenta mientras tengas solicitudes activas."
            }
            return@withContext "No se pudo eliminar la cuenta en el servidor."
        } catch (e: IOException) {
            return@withContext "Sin conexión: no se pudo eliminar la cuenta."
        } catch (e: Exception) {
            return@withContext "No se pudo eliminar la cuenta: ${e.localizedMessage}"
        }
        tokenStore.clear()
        firebaseService.firebaseSignOut()

        // Delete user's row inside local Room DB
        userProfileDao.deleteUserById(user.id)
        setCurrentUser(null)
        AppTelemetry.event("profile.delete")
        null
    }

    suspend fun walletHistory(): List<WalletTransactionDto> = withContext(Dispatchers.IO) {
        try {
            apiService.walletTransactions()
        } catch (e: Exception) {
            Log.e("GalibuRepository", "walletHistory falló en backend: ${e.localizedMessage}")
            emptyList()
        }
    }

    // --- Subscription ---

    suspend fun getSubscription(): SubscriptionDto = withContext(Dispatchers.IO) {
        try {
            apiService.getSubscription()
        } catch (e: Exception) {
            Log.e("GalibuRepository", "getSubscription falló: ${e.localizedMessage}")
            // Fallback a datos por defecto si falla
            SubscriptionDto(
                plan = "FREE",
                status = "NONE",
                active = false,
                canWork = true, // Permitir trabajar si no hay respuesta
                freeServicesTotal = 3,
                freeServicesUsed = 0,
                freeServicesRemaining = 3,
                monthlyPrice = 29900.0,
                yearlyPrice = 299000.0
            )
        }
    }

    /** Crea una orden de suscripción en Bold y devuelve el checkout para abrirlo. */
    suspend fun createSubscriptionCheckout(plan: String): BoldCheckoutResponse =
        withContext(Dispatchers.IO) {
            apiService.createSubscriptionCheckout(SubscriptionCheckoutRequest(plan))
        }

    /**
     * Consulta el estado de una orden Bold. El backend activa el plan Pro
     * (o acredita saldo en el flujo legado) al confirmarse el pago.
     */
    suspend fun boldOrderStatus(orderId: String): BoldOrderStatusResponse =
        withContext(Dispatchers.IO) {
            apiService.boldOrderStatus(orderId)
        }

    /** Cancela la renovación de la suscripción (el acceso sigue hasta el vencimiento). */
    suspend fun cancelSubscription(): SubscriptionDto = withContext(Dispatchers.IO) {
        try {
            val dto = apiService.cancelSubscription()
            refreshMe() // Actualiza perfil local con nuevo estado
            dto
        } catch (e: Exception) {
            Log.e("GalibuRepository", "cancelSubscription falló: ${e.localizedMessage}")
            throw e
        }
    }

    /** Re-baja el perfil (saldo) desde el backend sin disparar otros syncs. */
    suspend fun refreshMe() = withContext(Dispatchers.IO) {
        try {
            val me = apiService.getMe()
            userProfileDao.insertUser(me)
            _currentUser.value = me
        } catch (e: Exception) {
            Log.e("GalibuRepository", "refreshMe falló: ${e.localizedMessage}")
        }
    }

    suspend fun logout() {
        tokenStore.clear()
        firebaseService.firebaseSignOut()
        
        withContext(Dispatchers.IO) {
            try {
                // Completely purge local Room database to prevent caching or stale state issues for the next user session
                jobRequestDao.deleteAllJobs()
                userProfileDao.deleteAllUserProfiles()
                bidDao.deleteAllBids()
                chatMessageDao.deleteAllChatMessages()
                notificationDao.deleteAllNotifications()
            } catch (e: Exception) {
                Log.e("GalibuRepository", "Error clearing local database on logout: ${e.localizedMessage}")
            }
        }
        setCurrentUser(null)
        AppTelemetry.event("auth.logout")
    }

    /**
     * Solicita al backend enviar un código de 6 dígitos para restablecer la
     * contraseña. El backend no revela si el correo existe; las cuentas de
     * Google son ignoradas (se gestionan desde la cuenta de Google).
     */
    suspend fun forgotPassword(email: String): String? = withContext(Dispatchers.IO) {
        try {
            apiService.forgotPassword(com.galibu.core.data.remote.ForgotPasswordRequestDto(email.trim().lowercase()))
            null
        } catch (e: HttpException) {
            backendErrorMessage(e)
                ?: when (e.code()) {
                    503 -> "El envío de correos falló en el servidor. Inténtalo de nuevo más tarde."
                    400 -> "No se pudo enviar el código de recuperación. Inténtalo de nuevo."
                    else -> "No se pudo enviar el código de recuperación. Inténtalo de nuevo."
                }
        } catch (e: java.net.SocketTimeoutException) {
            "La solicitud tardó demasiado (servidor ocupado). Inténtalo de nuevo."
        } catch (e: java.net.UnknownHostException) {
            "Sin conexión: verifica tu internet."
        } catch (e: IOException) {
            if (e.message?.contains("timeout", ignoreCase = true) == true) {
                "La solicitud tardó demasiado (servidor ocupado). Inténtalo de nuevo."
            } else {
                "Sin conexión: no se pudo enviar el código. Verifica tu internet."
            }
        } catch (e: Exception) {
            "No se pudo enviar el código de recuperación. Inténtalo de nuevo."
        }
    }

    /**
     * Restablece la contraseña con el código de 6 dígitos enviado al correo.
     */
    suspend fun resetPassword(email: String, code: String, newPassword: String): String? = withContext(Dispatchers.IO) {
        try {
            apiService.resetPassword(
                com.galibu.core.data.remote.ResetPasswordRequestDto(
                    email.trim().lowercase(),
                    code.trim(),
                    newPassword
                )
            )
            null
        } catch (e: HttpException) {
            backendErrorMessage(e) ?: "Código inválido o expirado. Solicita uno nuevo."
        } catch (e: java.net.SocketTimeoutException) {
            "La solicitud tardó demasiado (servidor ocupado). Inténtalo de nuevo."
        } catch (e: java.net.UnknownHostException) {
            "Sin conexión: verifica tu internet."
        } catch (e: IOException) {
            if (e.message?.contains("timeout", ignoreCase = true) == true) {
                "La solicitud tardó demasiado (servidor ocupado). Inténtalo de nuevo."
            } else {
                "Sin conexión: no se pudo restablecer la contraseña."
            }
        } catch (e: Exception) {
            e.localizedMessage ?: "No se pudo restablecer la contraseña."
        }
    }

    suspend fun getDepartments(): List<FirebaseDepartment> = withContext(Dispatchers.IO) {
        try {
            apiService.geoTree().map { node ->
                FirebaseDepartment(name = node.name, municipalities = node.municipalities)
            }
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error cargando departamentos: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun getCategories(): List<FirebaseCategory> = withContext(Dispatchers.IO) {
        try {
            apiService.categories().map { FirebaseCategory(name = it.name) }
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error cargando categorías: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun addCategory(name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            apiService.addCategory(mapOf("name" to name.trim()))
            AppTelemetry.event("category.create", mapOf("name" to name.trim()))
            true
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error creando categoría: ${e.localizedMessage}")
            false
        }
    }
    fun isFirebaseInitialized() = firebaseService.isFirebaseInitialized()

    // --- Job Requests ---

    fun getAllProfessionals(): Flow<List<UserProfile>> =
        combine(userProfileDao.getProfessionalsFlow(), _currentUser) { list, user ->
            val currentId = user?.id
            if (currentId.isNullOrBlank()) list else list.filter { it.id != currentId }
        }

    fun getAllJobs(): Flow<List<JobRequest>> = jobRequestDao.getAllJobsFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getOpenJobs(): Flow<List<JobRequest>> {
        val userIdFlow = _currentUser.map { it?.id.orEmpty() }.distinctUntilChanged()
        return userIdFlow.flatMapLatest { userId ->
            if (userId.isEmpty()) {
                jobRequestDao.getOpenJobsFlow()
            } else {
                combine(
                    jobRequestDao.getAllJobsFlow(),
                    bidDao.getJobIdsWithBidsFlow(userId)
                ) { allJobs, bidJobIds ->
                    allJobs.filter { job ->
                        val isOpen = job.status == "OPEN"
                        val isNegotiatingWithMe = job.status == "NEGOTIATING" && (job.id in bidJobIds || job.professionalId == userId)
                        val isAcceptedByMe = job.status == "ACCEPTED" && job.professionalId == userId
                        val isInProgressByMe = job.status == "IN_PROGRESS" && job.professionalId == userId

                        isOpen || isNegotiatingWithMe || isAcceptedByMe || isInProgressByMe
                    }
                }
            }
        }
    }
    fun getJobsByClient(clientId: String): Flow<List<JobRequest>> = jobRequestDao.getJobsByClientFlow(clientId)
    fun getJobsByProfessional(proId: String): Flow<List<JobRequest>> = jobRequestDao.getJobsByProfessionalFlow(proId)
    fun getJobById(jobId: String): Flow<JobRequest?> = jobRequestDao.getJobByIdFlow(jobId)

    suspend fun insertJobDirectly(job: JobRequest) = withContext(Dispatchers.IO) {
        jobRequestDao.insertJob(job)
        if (!job.professionalId.isNullOrEmpty()) {
            val notification = AppNotification(
                userId = job.professionalId,
                title = "¡Nueva propuesta de contratación! 🛠️",
                body = "${job.clientName} te ha enviado una solicitud de servicio directo de ${job.category}.",
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            notificationDao.insertNotification(notification)
        }
    }

    suspend fun findActiveJobWithProfessional(proId: String): JobRequest? = withContext(Dispatchers.IO) {
        try {
            jobRequestDao.getActiveJobWithProfessionalSync(proId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error buscando job activo con profesional: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Resolves which job thread backs a chat with a professional:
     * 1. Si el profesional ya está asignado a la solicitud, se reutiliza ese job.
     * 2. Si el cliente contacta a un profesional que solo envió una oferta, se abre
     *    un hilo privado por (solicitud, profesional): el hilo público de la
     *    solicitud lo comparten todos los ofertantes y no sirve para negociar.
     * 3. Si no hay job explícito, reutiliza el trabajo activo más reciente con ese pro.
     * 4. Si no existe, crea un hilo de consulta directo (oculto de "solicitudes").
     * Devuelve el id del job o null si hace falta crear uno y no hay sesión.
     */
    suspend fun resolveChatJobId(
        proId: String,
        proName: String,
        proSpecialty: String,
        jobId: String? = null
    ): String? = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            if (!jobId.isNullOrBlank()) {
                val job = try { jobRequestDao.getJobByIdSync(jobId) } catch (e: Exception) { null }
                    ?: try {
                        apiService.getJob(jobId).also { remote ->
                            try { jobRequestDao.insertJob(remote) } catch (e: Exception) {
                                Log.e("GalibuRepository", "Error guardando job $jobId en Room: ${e.localizedMessage}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("GalibuRepository", "Job $jobId no está en backend: ${e.localizedMessage}")
                        null
                    }
                if (job != null) {
                    if (job.professionalId == proId) {
                        return@withTransaction job.id
                    }
                    val hasActiveOffer = try {
                        bidDao.getBidsForJobAndProfessionalSync(job.id, proId)
                            .any { !it.status.equals("REJECTED", ignoreCase = true) }
                    } catch (e: Exception) {
                        false
                    }
                    if (hasActiveOffer) {
                        return@withTransaction openNegotiationChat(job, proId, proName, proSpecialty)
                    }
                    return@withTransaction job.id
                }
            }
            findActiveJobWithProfessional(proId)?.let { return@withTransaction it.id }
            val user = _currentUser.value ?: return@withTransaction null
            val newJobId = "chat_${proId}_${System.currentTimeMillis()}"
            val created = try {
                apiService.createJob(
                    CreateJobDto(
                        id = newJobId,
                        category = proSpecialty,
                        title = "Consulta sobre $proSpecialty",
                        description = "Chat de consulta directa con el profesional $proName.",
                        budgetMin = 0.0,
                        address = user.address ?: "Ubicación del cliente",
                        department = user.department ?: "Bogotá D.C.",
                        municipality = user.municipality ?: "Bogotá",
                        professionalId = proId,
                        professionalName = proName
                    )
                )
            } catch (e: Exception) {
                Log.e("GalibuRepository", "No se pudo crear el hilo de chat en backend: ${e.localizedMessage}")
                AppTelemetry.error("chat", "No se pudo crear el hilo de chat", e)
                return@withTransaction null
            }
            jobRequestDao.insertJob(created)
            AppTelemetry.event("chat.start", mapOf("jobId" to created.id))
            return@withTransaction created.id
        }
    }

    /**
     * Hilo privado para negociar una oferta. El id es determinístico para
     * reutilizar el mismo chat cuando el cliente vuelve a tocar "Contactar".
     */
    private suspend fun openNegotiationChat(
        job: JobRequest,
        proId: String,
        proName: String,
        proSpecialty: String
    ): String? {
        val chatJobId = "chat_" + UUID.nameUUIDFromBytes("$proId|${job.id}".toByteArray())
            .toString().replace("-", "")
        val existing = try { jobRequestDao.getJobByIdSync(chatJobId) } catch (e: Exception) { null }
        if (existing != null) return existing.id
        val user = _currentUser.value
        val created = try {
            apiService.createJob(
                CreateJobDto(
                    id = chatJobId,
                    category = "Consulta",
                    title = "Consulta sobre ${job.category.ifBlank { proSpecialty }}",
                    description = "Chat privado para acordar el precio de una oferta.",
                    budgetMin = 0.0,
                    address = job.address.ifBlank { user?.address ?: "Ubicación del cliente" },
                    department = job.department ?: user?.department ?: "Bogotá D.C.",
                    municipality = job.municipality ?: user?.municipality ?: "Bogotá",
                    professionalId = proId,
                    professionalName = proName,
                    source = "PUBLISH"
                )
            )
        } catch (e: HttpException) {
            // Puede existir ya en el backend aunque todavía no esté en Room.
            try { apiService.getJob(chatJobId) } catch (e2: Exception) { null }
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo abrir el chat de negociación: ${e.localizedMessage}")
            AppTelemetry.error("chat", "No se pudo abrir el chat de negociación", e)
            null
        }
        if (created != null) {
            try { jobRequestDao.insertJob(created) } catch (e: Exception) {
                Log.e("GalibuRepository", "Error guardando chat de negociación: ${e.localizedMessage}")
            }
            AppTelemetry.event("chat.start", mapOf("jobId" to created.id, "offer" to job.id))
            return created.id
        }
        return null
    }

    suspend fun createJobRequest(category: String, title: String, description: String, budget: Double, address: String, department: String = "Bogotá D.C.", municipality: String = "Bogotá", suggestedPrice: Double? = null, professionalId: String? = null, professionalName: String? = null, source: String = "PUBLISH", photos: List<String>? = null) = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            val user = _currentUser.value ?: return@withTransaction

            // Sube las fotos adjuntas (data URIs) y conserva las que ya son URL.
            val finalPhotos = photos?.let { list ->
                val uploaded = mutableListOf<String>()
                list.forEachIndexed { index, photo ->
                    val value = photo.trim()
                    if (value.isBlank()) return@forEachIndexed
                    if (value.startsWith("http://") || value.startsWith("https://")) {
                        uploaded.add(value)
                        return@forEachIndexed
                    }
                    val bytes = if (value.startsWith("data:image")) {
                        try {
                            val comma = value.indexOf(',')
                            if (comma == -1) null
                            else android.util.Base64.decode(value.substring(comma + 1), android.util.Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                    if (bytes == null) {
                        uploaded.add(value)
                        return@forEachIndexed
                    }
                    val stored = try {
                        firebaseService.uploadWorkPhoto(
                            userId = user.id,
                            photoId = "job_${index}_${System.currentTimeMillis()}",
                            photoBytes = bytes
                        )
                    } catch (e: Exception) {
                        value
                    }
                    uploaded.add(stored)
                }
                uploaded
            }

            val created = try {
                apiService.createJob(
                    CreateJobDto(
                        category = category,
                        title = title,
                        description = description,
                        budgetMin = budget,
                        address = address,
                        department = department,
                        municipality = municipality,
                        suggestedPrice = suggestedPrice,
                        professionalId = professionalId,
                        professionalName = professionalName,
                        source = source,
                        photos = finalPhotos?.joinToString("|")
                    )
                )
            } catch (e: Exception) {
                Log.e("GalibuRepository", "createJobRequest falló en backend: ${e.localizedMessage}")
                AppTelemetry.error("jobs", "No se pudo crear la solicitud", e)
                throw e
            }
            jobRequestDao.insertJob(created)
            AppTelemetry.event(
                "job.create",
                mapOf(
                    "jobId" to created.id,
                    "source" to source,
                    "category" to category,
                    "hasProfessional" to (professionalId != null).toString()
                )
            )
        }
    }

    /**
     * Direct hire used by the client marketplace: reuses an active job with the same
     * professional or creates a NEGOTIATING direct-hire job. Returns the job id.
     */
    suspend fun hireProfessional(
        proId: String,
        proName: String,
        proSpecialty: String,
        hourlyRate: Double?,
        proBaseHourlyRate: Double? = null
    ): String? = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            val user = _currentUser.value ?: return@withTransaction null
            try {
                val existing = apiService.jobs(clientId = user.id, professionalId = proId, status = null)
                    .firstOrNull { it.status == "NEGOTIATING" || it.status == "OPEN" }
                if (existing != null) {
                    jobRequestDao.insertJob(existing)
                    AppTelemetry.event("job.hire", mapOf("jobId" to existing.id, "reused" to "true"))
                    return@withTransaction existing.id
                }
                // Sin presupuesto (0) la solicitud queda "precio a convenir" y el
                // profesional cotiza; la tarifa de su perfil solo es referencia.
                val rate = hourlyRate?.takeIf { it > 0 } ?: 0.0
                val proProfile = getUserById(proId)
                val baseHourlyRate = proBaseHourlyRate?.takeIf { it > 0 } ?: proProfile?.hourlyRate
                val created = apiService.createJob(
                    CreateJobDto(
                        category = proSpecialty,
                        title = "Contratación de $proSpecialty",
                        description = if (rate > 0) {
                            "Solicitud de servicio directo con el profesional $proName. Presupuesto propuesto: $${rate} COP."
                        } else {
                            "Solicitud de servicio directo con el profesional $proName. Precio a convenir."
                        },
                        budgetMin = rate,
                        address = user.address ?: "Calle 100 # 15-22",
                        department = user.department ?: "Bogotá D.C.",
                        municipality = user.municipality ?: "Bogotá",
                        professionalId = proId,
                        professionalName = proName,
                        proHourlyRate = baseHourlyRate
                    )
                )
                jobRequestDao.insertJob(created)
                AppTelemetry.event("job.hire", mapOf("jobId" to created.id, "category" to proSpecialty))
                return@withTransaction created.id
            } catch (e: Exception) {
                Log.e("GalibuRepository", "hireProfessional falló: ${e.localizedMessage}")
                AppTelemetry.error("jobs", "No se pudo contratar al profesional", e)
                throw e
            }
        }
    }

    // --- Price Bids & Negotiations ---

    /**
     * Ofertas visibles de un job. Las rechazadas se conservan en la base local
     * para auditoría, pero no deben aparecer en la negociación.
     */
    private fun List<Bid>.withoutRejectedOffers(): List<Bid> =
        filterNot { it.status.equals("REJECTED", ignoreCase = true) }

    fun getBidsForJob(jobId: String): Flow<List<Bid>> =
        bidDao.getBidsForJobFlow(jobId).map { it.withoutRejectedOffers() }

    suspend fun getBidsForJobSync(jobId: String): List<Bid> = withContext(Dispatchers.IO) {
        try {
            bidDao.getBidsForJobSync(jobId).withoutRejectedOffers()
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error leyendo bids locales de $jobId: ${e.localizedMessage}")
            emptyList()
        }
    }

    /**
     * Crea la notificación in-app de una nueva oferta recibida en una
     * solicitud del cliente. Devuelve la notificación creada.
     */
    suspend fun notifyNewBidOffer(
        clientId: String,
        job: JobRequest,
        bid: Bid,
        amountText: String
    ): AppNotification = withContext(Dispatchers.IO) {
        val title = job.title.takeIf { it.isNotBlank() } ?: job.category.ifBlank { "tu solicitud" }
        val comment = bid.comment.takeIf { it.isNotBlank() }?.let { " «$it»" } ?: ""
        val notification = AppNotification(
            userId = clientId,
            title = "¡Nueva oferta recibida! 💰",
            body = "${bid.professionalName} ofertó $amountText COP en '$title'.$comment",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            jobId = job.id
        )
        try {
            notificationDao.insertNotification(notification)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error guardando notificación de oferta: ${e.localizedMessage}")
        }
        notification
    }

    suspend fun markJobNotificationsAsRead(userId: String, jobId: String) = withContext(Dispatchers.IO) {
        try {
            notificationDao.markJobAsRead(userId, jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error marcando notificaciones del job: ${e.localizedMessage}")
        }
    }

    suspend fun getUserRatings(userId: String): List<com.galibu.core.data.remote.RatingDto> =
        withContext(Dispatchers.IO) {
            try {
                apiService.userRatings(userId)
            } catch (e: Exception) {
                Log.e("GalibuRepository", "getUserRatings falló: ${e.localizedMessage}")
                emptyList()
            }
        }

    suspend fun submitBid(jobId: String, amount: Double, comment: String, durationHours: Int) = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            _currentUser.value ?: return@withTransaction
            val saved = try {
                apiService.submitBid(
                    jobId,
                    SubmitBidDto(amount = amount, comment = comment, durationHours = durationHours)
                )
            } catch (e: Exception) {
                Log.e("GalibuRepository", "submitBid falló en backend: ${e.localizedMessage}")
                AppTelemetry.error("bids", "No se pudo enviar la oferta", e)
                throw e
            }
            bidDao.insertBid(saved)
            // Oferta persistida en backend; Firestore ya no es fuente de verdad para bids.

            // Refresh the job (the server flips OPEN -> NEGOTIATING on first bid).
            refreshJobDetails(jobId)
            AppTelemetry.event(
                "bid.submit",
                mapOf("jobId" to jobId, "bidId" to saved.id, "amount" to amount.toString())
            )
        }
    }

    /**
     * Counter offer negotiation in real-time
     */
    suspend fun proposeCounterOffer(bidId: String, counterAmount: Double) = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            val updated = try {
                apiService.counterBid(bidId, AmountDto(counterAmount))
            } catch (e: Exception) {
                Log.e("GalibuRepository", "proposeCounterOffer falló en backend: ${e.localizedMessage}")
                AppTelemetry.error("bids", "No se pudo enviar la contraoferta", e)
                throw e
            }
            bidDao.insertBid(updated)
            refreshJobDetails(updated.jobId)
            AppTelemetry.event(
                "bid.counter",
                mapOf("bidId" to bidId, "jobId" to updated.jobId, "amount" to counterAmount.toString())
            )
        }
    }

    suspend fun acceptBid(bidId: String) = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            val bid = bidDao.getBidByIdSync(bidId) ?: return@withTransaction
            acceptBidInternal(bidId, bid.professionalName, isBot = bid.professionalId.startsWith("bot_"))
            AppTelemetry.event("bid.accept", mapOf("bidId" to bidId, "jobId" to bid.jobId))
        }
    }

    private suspend fun acceptBidInternal(bidId: String, professionalName: String, isBot: Boolean) {
        val localBid = bidDao.getBidByIdSync(bidId) ?: return
        val accepted = try {
            apiService.acceptBid(bidId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "acceptBid falló en backend: ${e.localizedMessage}")
            throw e
        }
        bidDao.insertBid(accepted)
        refreshJobDetails(accepted.jobId)
        refreshBidsForJob(accepted.jobId)
        // El mensaje de sistema "Chat seguro iniciado" lo crea el backend y llega
        // a ambos participantes por el polling del chat.
    }

    suspend fun rejectBid(bidId: String) = withContext(Dispatchers.IO) {
        val rejected = try {
            apiService.rejectBid(bidId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "rejectBid falló en backend: ${e.localizedMessage}")
            AppTelemetry.error("bids", "No se pudo rechazar la oferta", e)
            throw e
        }
        bidDao.insertBid(rejected)
        AppTelemetry.event("bid.reject", mapOf("bidId" to bidId, "jobId" to rejected.jobId))
    }

    suspend fun cancelJob(jobId: String, reason: String = "") = withContext(Dispatchers.IO) {
        val localJob = jobRequestDao.getJobByIdSync(jobId)
        val wasAccepted = localJob?.status in listOf("ACCEPTED", "IN_PROGRESS", "PENDING_RATING")
        val cancelled = try {
            apiService.cancelJob(jobId, reason.ifBlank { null })
        } catch (e: Exception) {
            Log.e("GalibuRepository", "cancelJob falló en backend: ${e.localizedMessage}")
            throw e
        }
        jobRequestDao.insertJob(cancelled)

        // Delete local bids for this job
        try {
            bidDao.deleteBidsForJob(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error limpiando bids locales de $jobId: ${e.localizedMessage}")
        }
        // El aviso de cancelación en el chat lo crea el backend.
        AppTelemetry.event("job.cancel", mapOf("jobId" to jobId, "wasAccepted" to wasAccepted.toString()))
    }

    // --- Chat and Messages ---

    fun getChatMessagesForJob(jobId: String): Flow<List<ChatMessage>> = chatMessageDao.getMessagesForJobFlow(jobId)

    /** Todos los mensajes locales (para listas de conversaciones y conteo de no leídos). */
    fun getAllChatMessages(): Flow<List<ChatMessage>> = chatMessageDao.getAllMessagesFlow()

    fun getJobIdsWithBids(professionalId: String): Flow<List<String>> = bidDao.getJobIdsWithBidsFlow(professionalId)

    fun getBidsByProfessional(professionalId: String): Flow<List<Bid>> = bidDao.getBidsForProfessionalFlow(professionalId)

    suspend fun sendMessage(jobId: String, message: ChatMessage) = withContext(Dispatchers.IO) {
        val saved = apiService.sendChat(jobId, com.galibu.core.data.remote.CreateChatMessageRequest(
            text = message.text,
            type = message.type
        ))
        chatMessageDao.insertMessage(saved)
    }

    suspend fun sendChatMessage(jobId: String, text: String) = withContext(Dispatchers.IO) {
        val currentUserVal = _currentUser.value ?: return@withContext
        val msg = ChatMessage(
            jobId = jobId,
            senderId = currentUserVal.id,
            senderName = currentUserVal.name,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        sendMessage(jobId, msg)
        AppTelemetry.event("chat.message", mapOf("jobId" to jobId))
    }

    suspend fun sendImageMessage(jobId: String, imageBytes: ByteArray) = withContext(Dispatchers.IO) {
        val currentUserVal = _currentUser.value ?: return@withContext
        val msgId = java.util.UUID.randomUUID().toString()
        val imageUrl = firebaseService.uploadChatImage(jobId, msgId, imageBytes)
        
        val msg = ChatMessage(
            id = msgId,
            jobId = jobId,
            senderId = currentUserVal.id,
            senderName = currentUserVal.name,
            text = imageUrl,
            type = "image",
            timestamp = System.currentTimeMillis()
        )
        sendMessage(jobId, msg)
        AppTelemetry.event("chat.image", mapOf("jobId" to jobId))
    }

    suspend fun markAsRead(jobId: String) = withContext(Dispatchers.IO) {
        val currentUserId = _currentUser.value?.id ?: return@withContext
        // Actualiza el estado local de inmediato para que el contador de no leídos
        // desaparezca al abrir el chat, sin depender de la sincronización remota.
        chatMessageDao.markMessagesAsRead(jobId, currentUserId)
        try {
            apiService.markChatsRead(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "markAsRead falló en backend: ${e.localizedMessage}")
        }
    }

    suspend fun deleteMessage(jobId: String, messageId: String) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteChat(jobId, messageId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "deleteMessage falló en backend: ${e.localizedMessage}")
        }
        try { chatMessageDao.deleteMessage(messageId) } catch (e: Exception) {
            Log.e("GalibuRepository", "deleteMessage local falló: ${e.localizedMessage}")
        }
    }

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        Log.w("GalibuRepository", "deleteMessage(messageId) called but jobId is required for subcollection path jobs/jobId/chats/messageId")
    }

    suspend fun setTypingStatus(jobId: String, typing: Boolean) = withContext(Dispatchers.IO) {
        val currentUserId = _currentUser.value?.id ?: return@withContext
        firebaseService.setTypingStatusInCloud(jobId, currentUserId, typing)
    }

    fun listenTypingStatus(jobId: String): Flow<Map<String, Boolean>> = callbackFlow {
        val listener = firebaseService.listenToTypingStatus(jobId) { typingMap ->
            trySend(typingMap)
        }
        awaitClose {
            listener?.remove()
        }
    }

    // --- Payments and Completion ---

    /**
     * Llama al backend para finalizar el servicio. El backend cierra el job,
     * registra la calificación (opcional) y devuelve el job actualizado.
     * El pago del servicio es fuera de la app (efectivo/Nequi/transferencia).
     */
    suspend fun finishService(
        jobId: String,
        stars: Int? = null,
        comment: String? = null
    ): JobRequest = withContext(Dispatchers.IO) {
        val ratingReq = if (stars != null && stars in 1..5) {
            com.galibu.core.data.remote.RatingRequestDto(
                stars = stars,
                comment = comment?.takeIf { it.isNotBlank() }
            )
        } else {
            com.galibu.core.data.remote.RatingRequestDto(
                stars = 0,
                comment = null
            )
        }
        val updatedJob = try {
            apiService.finishService(jobId, ratingReq)
        } catch (e: HttpException) {
            // Si el otro participante ya finalizó (COMPLETED), este segundo
            // cierre solo registra la calificación sin volver a cerrar.
            if (e.code() == 409) {
                val remote = runCatching { apiService.getJob(jobId) }.getOrNull()
                if (remote?.status == "COMPLETED") {
                    // Solo registra la calificación si corresponde
                    if (stars != null && stars in 1..5) {
                        try {
                            apiService.rateJob(jobId, ratingReq)
                        } catch (rateError: HttpException) {
                            if (rateError.code() != 409) throw rateError
                        }
                    }
                    val refreshed = runCatching { apiService.getJob(jobId) }.getOrNull()
                    if (refreshed != null) {
                        jobRequestDao.insertJob(refreshed)
                        return@withContext refreshed
                    }
                }
            }
            Log.e("GalibuRepository", "finishService falló en backend: ${e.localizedMessage}")
            throw e
        }
        jobRequestDao.insertJob(updatedJob)
        // Notificación local (el backend también envía push)
        val currentUser = _currentUser.value
        if (currentUser != null) {
            val isClient = currentUser.id == updatedJob.clientId
            val title = if (isClient) "Servicio finalizado" else "Servicio finalizado"
            val body = "El trabajo '${updatedJob.title}' ha sido finalizado. ${if (stars != null) "Calificación: $stars ⭐" else "Pendiente de calificación."}"
            val alert = AppNotification(
                userId = currentUser.id,
                title = title,
                body = body,
                jobId = jobId
            )
            notificationDao.insertNotification(alert)
        }
        AppTelemetry.event("job.finish", mapOf("jobId" to jobId, "stars" to (stars ?: 0).toString()))
        updatedJob
    }

    suspend fun submitServiceRating(jobId: String, rating: Float, review: String, isClientRating: Boolean) = withContext(Dispatchers.IO) {
        val job = jobRequestDao.getJobByIdSync(jobId) ?: return@withContext
        // Evita doble envío/doble liquidación si el usuario reintenta.
        if (isClientRating && job.clientRatingOfPro != null) return@withContext
        if (!isClientRating && job.proRatingOfClient != null) return@withContext

        // 1. Persistencia en backend (estrellas enteras 1-5). Si falla (offline),
        // se conserva solo local y el sync posterior lo mantiene visible.
        try {
            apiService.rateJob(
                jobId,
                com.galibu.core.data.remote.RatingRequestDto(
                    stars = rating.toInt().coerceIn(1, 5),
                    comment = review.ifBlank { null }
                )
            )
        } catch (e: Exception) {
            Log.e("GalibuRepository", "submitServiceRating backend falló, se conserva local: ${e.localizedMessage}")
        }
        val finalJob = job
        val finalAddress = if (finalJob.address.isNotBlank()) finalJob.address else if (job.address.isNotBlank()) job.address else "Cl. 51 #62-25, Rionegro, Antioquia, Colombia"
        
        val updatedJob = if (isClientRating) {
            finalJob.copy(
                clientRatingOfPro = rating,
                clientReviewOfPro = review,
                completionRating = rating,
                completionReview = review,
                address = finalAddress
            )
        } else {
            finalJob.copy(
                proRatingOfClient = rating,
                proReviewOfClient = review,
                address = finalAddress
            )
        }
        
        jobRequestDao.insertJob(updatedJob)
        // La calificación ya quedó persistida (backend + local): un fallo de
        // Firestore no debe mostrarse como error al usuario.

        // Re-calculate average ratings
        if (isClientRating) {
            val proId = job.professionalId ?: return@withContext
            val proProfile = userProfileDao.getUserByIdSync(proId)
            if (proProfile != null) {
                val currentRating = proProfile.rating
                val jobsCount = proProfile.completedJobs + 1
                val newAverage = ((currentRating * (jobsCount - 1)) + rating) / jobsCount
                val updatedPro = proProfile.copy(rating = newAverage)
                userProfileDao.insertUser(updatedPro)
            }
        } else {
            val clientId = job.clientId
            val clientProfile = userProfileDao.getUserByIdSync(clientId)
            if (clientProfile != null) {
                val currentRating = clientProfile.rating
                val jobsCount = clientProfile.completedJobs + 1
                val newAverage = ((currentRating * (jobsCount - 1)) + rating) / jobsCount
                val updatedClient = clientProfile.copy(rating = newAverage)
                userProfileDao.insertUser(updatedClient)
            }
        }

        AppTelemetry.event(
            "job.rate",
            mapOf(
                "jobId" to jobId,
                "stars" to rating.toInt().toString(),
                "ratedBy" to if (isClientRating) "CLIENT" else "PROFESSIONAL"
            )
        )
    }

    /**
     * Traduce un rechazo del backend al registrar a un mensaje claro para el
     * usuario (sin códigos). Detecta la causa por el cuerpo del error.
     */
    private fun friendlyRegisterError(e: HttpException): String {
        val body = try {
            e.response()?.errorBody()?.string().orEmpty()
        } catch (ignored: Exception) {
            ""
        }
        val t = (body + " " + e.localizedMessage.orEmpty()).lowercase()
        val emailTaken = e.code() == 409 || "email_exists" in t ||
            (("correo" in t || "email" in t) && ("existe" in t || "uso" in t || "registr" in t || "duplic" in t)) ||
            ("already" in t && "exist" in t)
        return when {
            emailTaken -> "EMAIL_EXISTS"
            "password" in t || "contrase" in t ->
                "La contraseña debe tener mínimo 8 caracteres."
            "email" in t || "correo" in t ->
                "El correo electrónico no es válido o ya está en uso."
            "name" in t || "nombre" in t ->
                "El nombre es obligatorio."
            "role" in t || "rol" in t ->
                "El tipo de cuenta no es válido para esta aplicación."
            e.code() == 400 ->
                "Revisa los datos ingresados: alguno no cumple los requisitos."
            else -> "No se pudo crear la cuenta. Inténtalo de nuevo."
        }
    }

    // --- Push Notifications Center ---

    /**
     * Reactivo al usuario actual: antes se ataba al id del momento de crear
     * el ViewModel (vacío pre-login) y la campanita quedaba en 0 para siempre.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNotificationsFlow(): Flow<List<AppNotification>> {
        return _currentUser
            .map { it?.id ?: "" }
            .distinctUntilChanged()
            .flatMapLatest { userId ->
                if (userId.isEmpty()) flowOf(emptyList())
                else notificationDao.getNotificationsForUserFlow(userId)
            }
    }

    suspend fun markNotificationAsRead(id: Int) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun acceptDirectHire(jobId: String) = withContext(Dispatchers.IO) {
        val updatedJob = try {
            apiService.acceptDirect(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "acceptDirectHire falló en backend: ${e.localizedMessage}")
            throw e
        }
        jobRequestDao.insertJob(updatedJob)
        // El mensaje "Chat seguro iniciado" lo crea el backend.
        AppTelemetry.event("job.accept_direct", mapOf("jobId" to jobId))
    }

    suspend fun rejectDirectHire(jobId: String) = withContext(Dispatchers.IO) {
        val updatedJob = try {
            apiService.rejectDirect(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "rejectDirectHire falló en backend: ${e.localizedMessage}")
            throw e
        }
        jobRequestDao.insertJob(updatedJob)
        AppTelemetry.event("job.reject_direct", mapOf("jobId" to jobId))
    }

    suspend fun startService(jobId: String) = withContext(Dispatchers.IO) {
        val updatedJob = try {
            apiService.startService(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "startService falló en backend: ${e.localizedMessage}")
            throw e
        }
        jobRequestDao.insertJob(updatedJob)
        // El mensaje "servicio iniciado" lo crea el backend.
        AppTelemetry.event("job.start", mapOf("jobId" to jobId))
    }

    suspend fun finishService(jobId: String, stars: Int, comment: String) = withContext(Dispatchers.IO) {
        try {
            val updatedJob = apiService.finishService(
                jobId,
                com.galibu.core.data.remote.RatingRequestDto(
                    stars = stars.coerceIn(1, 5),
                    comment = comment.ifBlank { null }
                )
            )
            jobRequestDao.insertJob(updatedJob)
            // El mensaje "servicio finalizado" lo crea el backend.
            AppTelemetry.event("job.finish", mapOf("jobId" to jobId, "stars" to stars.toString()))
        } catch (e: HttpException) {
            // El otro participante ya finalizó el servicio (COMPLETED): este
            // segundo cierre solo registra la calificación, sin volver a cerrar.
            val remote = if (e.code() == 409) {
                runCatching { apiService.getJob(jobId) }.getOrNull()
            } else {
                null
            }
            if (remote?.status == "COMPLETED") {
                try {
                    apiService.rateJob(
                        jobId,
                        com.galibu.core.data.remote.RatingRequestDto(
                            stars = stars.coerceIn(1, 5),
                            comment = comment.ifBlank { null }
                        )
                    )
                } catch (rateError: HttpException) {
                    // 409 = ya calificó antes (reintento): la operación es idempotente.
                    if (rateError.code() != 409) throw rateError
                }
                val refreshed = runCatching { apiService.getJob(jobId) }.getOrNull()
                if (refreshed != null) jobRequestDao.insertJob(refreshed)
                AppTelemetry.event("job.rate_after_finish", mapOf("jobId" to jobId, "stars" to stars.toString()))
            } else {
                Log.e("GalibuRepository", "finishService falló en backend: ${e.localizedMessage}")
                throw e
            }
        }
    }

    suspend fun cancelJobRequest(jobId: String) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteJob(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "cancelJobRequest falló en backend: ${e.localizedMessage}")
            throw e
        }
        // Delete from local DB
        jobRequestDao.deleteJobById(jobId)
        try {
            bidDao.deleteBidsForJob(jobId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Error limpiando bids locales de $jobId: ${e.localizedMessage}")
        }
        AppTelemetry.event("job.delete", mapOf("jobId" to jobId))
    }

    suspend fun submitCounterOfferDirectHire(jobId: String, amount: Double, isClient: Boolean) = withContext(Dispatchers.IO) {
        val updatedJob = try {
            apiService.counterJob(jobId, AmountDto(amount))
        } catch (e: Exception) {
            Log.e("GalibuRepository", "submitCounterOfferDirectHire falló en backend: ${e.localizedMessage}")
            throw e
        }
        jobRequestDao.insertJob(updatedJob)

        // El mensaje "nueva tarifa propuesta" lo crea el backend.
        AppTelemetry.event(
            "job.counter_direct",
            mapOf("jobId" to jobId, "amount" to amount.toString(), "sender" to if (isClient) "CLIENT" else "PROFESSIONAL")
        )
    }

    private suspend fun processProfilePhotoToBase64(context: Context, photoUrl: String?, defaultPhoto: String): String {
        val targetUrl = if (photoUrl.isNullOrEmpty()) defaultPhoto else photoUrl
        
        if (targetUrl.startsWith("data:image/")) {
            return targetUrl
        }
        
        if (targetUrl.startsWith("http")) {
            val base64 = com.galibu.core.ui.utils.ImageUtils.downloadUrlToBase64(targetUrl)
            return base64 ?: targetUrl
        }
        
        if (targetUrl.startsWith("content://") || targetUrl.startsWith("file://")) {
            return try {
                val uri = targetUrl.toUri()
                val base64 = com.galibu.core.ui.utils.ImageUtils.uriToBase64(context, uri)
                base64 ?: targetUrl
            } catch (e: Exception) {
                targetUrl
            }
        }
        
        val base64 = com.galibu.core.ui.utils.ImageUtils.filePathToBase64(targetUrl)
        return base64 ?: targetUrl
    }
}


