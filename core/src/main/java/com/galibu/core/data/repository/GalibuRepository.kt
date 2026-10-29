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
import com.galibu.core.data.remote.PasswordChangeDto
import com.galibu.core.data.remote.RefreshRequestDto
import com.galibu.core.data.remote.RegisterRequestDto
import com.galibu.core.data.remote.SubmitBidDto
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
            false
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
        try {
            val expanded = apiService.addRole(AddRoleDto(intendedRole))
            val profile = try { apiService.getMe() } catch (e: Exception) {
                Log.e("GalibuRepository", "No se pudo releer el perfil tras expandir el rol: ${e.localizedMessage}")
                expanded
            }
            userProfileDao.insertUser(profile)
            setCurrentUser(profile)
            true
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo expandir el rol a $intendedRole: ${e.localizedMessage}")
            false
        }
    }

    /** Timestamp de la última ejecución de refreshJobs (para throttle). */
    private var lastRefreshJobsMs = 0L
    private val REFRESH_JOBS_THROTTLE_MS = 60_000L

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
        workDescription = workDescription,
        documentType = documentType,
        documentPhotoUrl = documentPhotoSimulatedUrl
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
    ): Boolean = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
        // Backend Google sign-in: finds or creates the user server-side and returns JWT.
        // Without an ID token there is no session to establish, so refuse (no ghost logins).
        if (idToken.isNullOrEmpty()) {
            Log.w("GalibuRepository", "loginWithGoogle sin ID token; se rechaza el ingreso para evitar sesión fantasma.")
            AppTelemetry.warn("auth", "Google login sin ID token")
            return@withTransaction false
        }
        val normalizedEmail = email.trim().lowercase()
        val auth = try {
            apiService.google(GoogleLoginRequestDto(idToken, role))
        } catch (e: HttpException) {
            Log.e("GalibuRepository", "Google login rechazado (${e.code()}): ${e.localizedMessage}")
            AppTelemetry.warn("auth", "Google login rechazado (${e.code()})")
            return@withTransaction false
        } catch (e: IOException) {
            Log.e("GalibuRepository", "Google login sin conexión: ${e.localizedMessage}")
            AppTelemetry.error("auth", "Google login sin conexión", e)
            return@withTransaction false
        } catch (e: Exception) {
            Log.e("GalibuRepository", "Google login falló: ${e.localizedMessage}")
            AppTelemetry.error("auth", "Google login falló", e)
            return@withTransaction false
        }
        tokenStore.saveTokens(auth.accessToken, auth.refreshToken)
        var backendUser = auth.user ?: return@withTransaction false

        // Google: si la cuenta ya existía con otro rol, se habilita el rol de
        // esta app en el backend (pasa a BOTH). Antes solo se cambiaba en local
        // y el siguiente getMe() lo revertía.
        if (backendUser.role != role && backendUser.role != "BOTH") {
            backendUser = try {
                val expanded = apiService.addRole(AddRoleDto(role))
                try { apiService.getMe() } catch (me: Exception) {
                    Log.e("GalibuRepository", "No se pudo releer el perfil tras expandir el rol (Google): ${me.localizedMessage}")
                    expanded
                }
            } catch (e: Exception) {
                Log.e("GalibuRepository", "No se pudo expandir el rol (Google) a $role: ${e.localizedMessage}")
                AppTelemetry.error("auth", "Google: fallo al expandir rol", e)
                tokenStore.clear()
                return@withTransaction false
            }
        }
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
            return@withTransaction true
        }
        Log.w("GalibuRepository", "Inalcanzable: el backend no retornó usuario en loginWithGoogle.")
        return@withTransaction false
        }
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
            var profile = auth.user
            if (profile == null) {
                tokenStore.clear()
                AppTelemetry.warn("auth", "Login sin perfil en la respuesta")
                return@withTransaction "No se pudo iniciar sesión. Inténtalo nuevamente."
            }
            // Expansión automática: la cuenta ya existe con otro rol, así que se
            // habilita el rol que pide esta app (el usuario pasa a BOTH).
            if (profile.role != role && profile.role != "BOTH") {
                profile = try {
                    val expanded = apiService.addRole(AddRoleDto(role))
                    try { apiService.getMe() } catch (me: Exception) {
                        Log.e("GalibuRepository", "No se pudo releer el perfil tras expandir el rol: ${me.localizedMessage}")
                        expanded
                    }
                } catch (e: Exception) {
                    Log.e("GalibuRepository", "No se pudo expandir el rol a $role: ${e.localizedMessage}")
                    AppTelemetry.error("auth", "Fallo al expandir rol", e)
                    tokenStore.clear()
                    return@withTransaction "No pudimos habilitar tu perfil como " +
                        "${if (role == "CLIENT") "Cliente" else "Profesional"}. " +
                        "Verifica tu conexión e inténtalo de nuevo."
                }
            }
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
                val resp = apiService.google(GoogleLoginRequestDto(googleIdToken, role))
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
                    tokenStore.saveTokens(loginResp.accessToken, loginResp.refreshToken)
                    val existing = loginResp.user ?: throw IllegalStateException("EMAIL_EXISTS")
                    if (existing.role == role || existing.role == "BOTH") {
                        throw IllegalStateException("EMAIL_EXISTS")
                    }
                    apiService.addRole(AddRoleDto(role))
                    try {
                        apiService.getMe()
                    } catch (meErr: Exception) {
                        Log.e("GalibuRepository", "No se pudo releer perfil tras expansión de rol: ${meErr.localizedMessage}")
                        existing.copy(role = "BOTH")
                    }
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
        setCurrentUser(synced)
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
        try {
            apiService.changePassword(PasswordChangeDto(currentPassword, newPassword))
            AppTelemetry.event("profile.password")
            null
        } catch (e: HttpException) {
            if (e.code() == 401) "La contraseña actual es incorrecta"
            else "No se pudo cambiar la contraseña en el servidor."
        } catch (e: IOException) {
            "Sin conexión: no se pudo cambiar la contraseña."
        } catch (e: Exception) {
            "No se pudo cambiar la contraseña: ${e.localizedMessage}"
        }
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

    suspend fun rechargeBalance(amount: Double) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val tx = try {
            apiService.rechargeWallet(AmountDto(amount))
        } catch (e: Exception) {
            Log.e("GalibuRepository", "rechargeBalance falló en backend: ${e.localizedMessage}")
            throw e
        }
        val updated = user.copy(balance = tx.balanceAfter)
        userProfileDao.insertUser(updated)
        // Firebase ya no es fuente de verdad para el perfil: el backend (MySQL) lo es.
        setCurrentUser(updated)
        
        // Add push notification
        val alert = AppNotification(
            userId = user.id,
            title = "Recarga de Saldo Exitosa 💰",
            body = "Has ingresado $$amount COP a tu billetera virtual de Galibu de forma inmediata."
        )
        notificationDao.insertNotification(alert)
        AppTelemetry.event("wallet.recharge", mapOf("amount" to amount.toString()))
    }

    suspend fun walletHistory(): List<WalletTransactionDto> = withContext(Dispatchers.IO) {
        try {
            apiService.walletTransactions()
        } catch (e: Exception) {
            Log.e("GalibuRepository", "walletHistory falló en backend: ${e.localizedMessage}")
            emptyList()
        }
    }

    /** Crea una orden de recarga en Bold y devuelve el checkout para abrirlo. */
    suspend fun createBoldCheckout(amount: Double): BoldCheckoutResponse =
        withContext(Dispatchers.IO) {
            apiService.createBoldCheckout(AmountDto(amount))
        }

    /** Consulta el estado de una orden Bold (el backend acredita el saldo). */
    suspend fun boldOrderStatus(orderId: String): BoldOrderStatusResponse =
        withContext(Dispatchers.IO) {
            apiService.boldOrderStatus(orderId)
        }

    suspend fun boldOrders(): List<com.galibu.core.data.remote.BoldOrderDto> =
        withContext(Dispatchers.IO) {
            try {
                apiService.boldOrders()
            } catch (e: Exception) {
                Log.e("GalibuRepository", "boldOrders falló: ${e.localizedMessage}")
                emptyList()
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

    suspend fun sendPasswordResetEmail(email: String) = withContext(Dispatchers.IO) {
        if (firebaseService.isFirebaseInitialized()) {
            firebaseService.firebaseSendPasswordResetEmail(email)
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

    // --- Document Verification ---

    suspend fun submitVerificationDocuments(docType: String, docBytes: ByteArray) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val docUrl = firebaseService.uploadIdentityDocument(user.id, docBytes)

        val updatedUser = try {
            apiService.submitVerification(
                com.galibu.core.data.remote.VerificationSubmitDto(docType, docUrl)
            )
        } catch (e: Exception) {
            Log.e("GalibuRepository", "submitVerification backend falló, se conserva local: ${e.localizedMessage}")
            user.copy(
                documentType = docType,
                documentPhotoSimulatedUrl = docUrl,
                verificationStatus = "PENDING"
            )
        }
        userProfileDao.insertUser(updatedUser)
        // Perfil persistido en backend; Firebase ya no es fuente de verdad.
        setCurrentUser(updatedUser)
        AppTelemetry.event("profile.verification", mapOf("docType" to docType))
    }

    suspend fun completeVerification() = withContext(Dispatchers.IO) {
        val updated = try {
            apiService.completeVerification()
        } catch (e: Exception) {
            Log.e("GalibuRepository", "completeVerification backend falló: ${e.localizedMessage}")
            val user = _currentUser.value ?: return@withContext null
            user.copy(verificationStatus = "VERIFIED", isVerified = true)
        }
        if (updated != null) {
            userProfileDao.insertUser(updated)
            setCurrentUser(updated)
        }
        updated
    }

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
     * Resolves which job thread backs a chat with a professional, avoiding duplicate
     * "chat_*" phantom jobs showing up as extra solicitudes:
     * 1. Explicit jobId (e.g. Contactar desde una oferta) -> reuse it (Room, else cloud).
     * 2. Otherwise the newest active (not COMPLETED/CANCELLED) job with that professional.
     * 3. Otherwise creates a fresh direct-chat thread (hidden from solicitudes via isDirectChat).
     * Returns the job id, or null when a new thread is needed but there is no signed-in user.
     */
    suspend fun resolveChatJobId(proId: String, proName: String, proSpecialty: String, jobId: String? = null): String? = withContext(Dispatchers.IO) {
        RequestTrace.withTransaction {
            if (!jobId.isNullOrBlank()) {
                val local = try { jobRequestDao.getJobByIdSync(jobId) } catch (e: Exception) { null }
                if (local != null) return@withTransaction local.id
                val backend = try {
                    apiService.getJob(jobId)
                } catch (e: Exception) {
                    Log.e("GalibuRepository", "Job $jobId no está en backend: ${e.localizedMessage}")
                    null
                }
                if (backend != null) {
                    try { jobRequestDao.insertJob(backend) } catch (e: Exception) {
                        Log.e("GalibuRepository", "Error guardando job $jobId en Room: ${e.localizedMessage}")
                    }
                    return@withTransaction backend.id
                }
                // Legacy direct-chat threads may live only in Firestore.
                val cloud = try {
                    firebaseService.getJobFromCloud(jobId)
                } catch (e: Exception) {
                    Log.e("GalibuRepository", "Error descargando job $jobId para chat: ${e.localizedMessage}")
                    null
                }
                if (cloud != null) {
                    try { jobRequestDao.insertJob(cloud) } catch (e: Exception) {
                        Log.e("GalibuRepository", "Error guardando job $jobId en Room: ${e.localizedMessage}")
                    }
                    return@withTransaction cloud.id
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
                val rate = hourlyRate ?: 35000.0
                val proProfile = getUserById(proId)
                val baseHourlyRate = proBaseHourlyRate ?: proProfile?.hourlyRate ?: 35000.0
                val created = apiService.createJob(
                    CreateJobDto(
                        category = proSpecialty,
                        title = "Contratación de $proSpecialty",
                        description = "Solicitud de servicio directo con el profesional $proName. Tarifa propuesta: $${rate}/hora.",
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
            body = "${bid.professionalName} ofertó $amountText COP/hora en '$title'.$comment",
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

        // Send entry chat system message (chat stays on Firebase).
        val welcomeMsg = ChatMessage(
            jobId = accepted.jobId,
            senderId = "system",
            senderName = "Sistema",
            text = "¡Presupuesto pactado en $$${accepted.amount} COP! Chat seguro iniciado. Coordinen los detalles aquí."
        )
        chatMessageDao.insertMessage(welcomeMsg)
        try {
            firebaseService.sendChatMessageToCloud(welcomeMsg)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo enviar mensaje de sistema al chat: ${e.localizedMessage}")
        }
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
        val currentUser = _currentUser.value
        val wasAccepted = localJob?.status in listOf("ACCEPTED", "IN_PROGRESS", "PENDING_RATING")
        val cancelled = try {
            apiService.cancelJob(jobId)
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

        // Send system message in chat if chat exists (chat stays on Firebase)
        if (wasAccepted && currentUser != null) {
            val systemMsg = ChatMessage(
                jobId = jobId,
                senderId = "system",
                senderName = "Sistema",
                text = "⚠️ Este trabajo ha sido cancelado por ${currentUser.name}${if (reason.isNotEmpty()) ": $reason" else ""}."
            )
            chatMessageDao.insertMessage(systemMsg)
            try {
                firebaseService.sendChatMessageToCloud(systemMsg)
            } catch (e: Exception) {
                Log.e("GalibuRepository", "No se pudo enviar mensaje de sistema al chat: ${e.localizedMessage}")
            }
        }
        AppTelemetry.event("job.cancel", mapOf("jobId" to jobId, "wasAccepted" to wasAccepted.toString()))
    }

    // --- Chat and Messages ---

    fun getChatMessagesForJob(jobId: String): Flow<List<ChatMessage>> = chatMessageDao.getMessagesForJobFlow(jobId)

    /** Todos los mensajes locales (para listas de conversaciones y conteo de no leídos). */
    fun getAllChatMessages(): Flow<List<ChatMessage>> = chatMessageDao.getAllMessagesFlow()

    fun getJobIdsWithBids(professionalId: String): Flow<List<String>> = bidDao.getJobIdsWithBidsFlow(professionalId)

    fun getBidsByProfessional(professionalId: String): Flow<List<Bid>> = bidDao.getBidsForProfessionalFlow(professionalId)

    suspend fun sendMessage(jobId: String, message: ChatMessage) = withContext(Dispatchers.IO) {
        try {
            val saved = apiService.sendChat(jobId, com.galibu.core.data.remote.CreateChatMessageRequest(
                text = message.text,
                type = message.type
            ))
            chatMessageDao.insertMessage(saved)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "sendMessage backend falló, se intenta Firebase: ${e.localizedMessage}")
            try {
                firebaseService.sendChatMessageToCloud(message)
                chatMessageDao.insertMessage(message)
            } catch (fe: Exception) {
                Log.e("GalibuRepository", "sendMessage Firebase también falló: ${fe.localizedMessage}")
            }
        }
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
            Log.e("GalibuRepository", "markAsRead backend falló, se intenta Firebase: ${e.localizedMessage}")
            firebaseService.markChatMessagesAsReadInCloud(jobId, currentUserId)
        }
    }

    suspend fun deleteMessage(jobId: String, messageId: String) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteChat(jobId, messageId)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "deleteMessage backend falló, se intenta Firebase: ${e.localizedMessage}")
            firebaseService.deleteChatMessageFromCloud(jobId, messageId)
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
     * Set job request status to PENDING_RATING so both users must rate each other before closing.
     */
    suspend fun startJobCompletion(jobId: String) = withContext(Dispatchers.IO) {
        val job = jobRequestDao.getJobByIdSync(jobId) ?: return@withContext
        if (job.status == "PENDING_RATING" || job.status == "COMPLETED") return@withContext

        val finalJob = job
        val finalAddress = if (finalJob.address.isNotBlank()) finalJob.address else if (job.address.isNotBlank()) job.address else "Cl. 51 #62-25, Rionegro, Antioquia, Colombia"
        val updatedJob = finalJob.copy(
            status = "PENDING_RATING",
            address = finalAddress
        )
        jobRequestDao.insertJob(updatedJob)

        // System message in chat
        val systemMsg = ChatMessage(
            jobId = jobId,
            senderId = "system",
            senderName = "Sistema",
            text = "Servicio finalizado en campo. Se encuentra en estado 'Pendiente de Calificación' hasta que ambos usuarios (Cliente y Profesional) se califiquen mutuamente."
        )
        chatMessageDao.insertMessage(systemMsg)
        firebaseService.sendChatMessageToCloud(systemMsg)

        // Create alert notifications
        val alertClient = AppNotification(
            userId = job.clientId,
            title = "Calificación Pendiente ⚠️",
            body = "El servicio '${job.title}' requiere tu calificación. Califica al profesional para completar el cierre."
        )
        notificationDao.insertNotification(alertClient)

        if (job.professionalId != null) {
            val alertPro = AppNotification(
                userId = job.professionalId,
                title = "Calificación Pendiente ⚠️",
                body = "El servicio '${job.title}' requiere tu calificación. Califica al cliente para completar el cierre."
            )
            notificationDao.insertNotification(alertPro)
        }
    }

    suspend fun simulateJobPayment(jobId: String, paymentMethod: String) = withContext(Dispatchers.IO) {
        finalizeAndPayJob(jobId, paymentMethod)
    }

    suspend fun finalizeAndPayJob(jobId: String, paymentMethod: String = "Transferencia / Nequi") = withContext(Dispatchers.IO) {
        val job = jobRequestDao.getJobByIdSync(jobId) ?: return@withContext
        val finalPrice = job.finalPrice ?: job.budgetMin

        // Deduct money from client and add to professional
        val client = userProfileDao.getUserByIdSync(job.clientId)
        val pro = if (job.professionalId != null) userProfileDao.getUserByIdSync(job.professionalId) else null

        if (client != null) {
            val updatedClient = client.copy(
                balance = (client.balance - finalPrice).coerceAtLeast(0.0),
                completedJobs = client.completedJobs + 1
            )
            userProfileDao.insertUser(updatedClient)
            if (_currentUser.value?.id == client.id) {
                setCurrentUser(updatedClient)
            }
        }

        if (pro != null) {
            val updatedPro = pro.copy(
                balance = pro.balance + finalPrice,
                completedJobs = pro.completedJobs + 1
            )
            userProfileDao.insertUser(updatedPro)
            if (_currentUser.value?.id == pro.id) {
                setCurrentUser(updatedPro)
            }
        }

        // Complete job request in DB. Firestore es best-effort: si falla, el
        // cierre local ya quedó aplicado y no debe romper el flujo del usuario.
        val finalJob = job
        val finalAddress = if (finalJob.address.isNotBlank()) finalJob.address else if (job.address.isNotBlank()) job.address else "Cl. 51 #62-25, Rionegro, Antioquia, Colombia"
        val completedJob = finalJob.copy(
            status = "COMPLETED",
            address = finalAddress
        )
        jobRequestDao.insertJob(completedJob)

        // System message in chat
        val systemMsg = ChatMessage(
            jobId = jobId,
            senderId = "system",
            senderName = "Sistema Pagos",
            text = "¡Ambas calificaciones completadas! Pago de $${String.format(Locale.US, "%,.0f", finalPrice)} COP liberado con éxito usando $paymentMethod. El servicio ha sido marcado como COMPLETADO."
        )
        chatMessageDao.insertMessage(systemMsg)
        try {
            firebaseService.sendChatMessageToCloud(systemMsg)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo sincronizar el mensaje de pago en Firestore: ${e.localizedMessage}")
            AppTelemetry.error("payment", "No se pudo sincronizar el mensaje de pago en Firestore", e)
        }

        // Create alert notifications
        val alertClient = AppNotification(
            userId = job.clientId,
            title = "Servicio Completado 🏆",
            body = "El pago a ${job.professionalName} ha sido liberado exitosamente. ¡Gracias por calificar!"
        )
        notificationDao.insertNotification(alertClient)

        if (job.professionalId != null) {
            val alertPro = AppNotification(
                userId = job.professionalId,
                title = "Pago recibido ($${String.format(Locale.US, "%,.0f", finalPrice)} COP) 💰",
                body = "¡Buen trabajo! El servicio se cerró y el pago fue liberado vía $paymentMethod."
            )
            notificationDao.insertNotification(alertPro)
        }
        AppTelemetry.event("job.payment_released", mapOf("jobId" to jobId, "amount" to finalPrice.toString()))
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
                completionRating = rating, // keep for backward compatibility
                completionReview = review, // keep for backward compatibility
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

        // Check if BOTH ratings are now submitted. Se revisa también la copia
        // de la nube porque el sync local puede llegar un instante después que
        // la calificación de la otra parte.
        val clientRated = updatedJob.clientRatingOfPro != null || finalJob.clientRatingOfPro != null
        val proRated = updatedJob.proRatingOfClient != null || finalJob.proRatingOfClient != null
        if (clientRated && proRated) {
            // La liquidación es posterior a la calificación ya guardada. Si algo
            // falla aquí se registra, pero no se convierte en error de calificar.
            try {
                finalizeAndPayJob(jobId)
            } catch (e: Exception) {
                Log.e("GalibuRepository", "submitServiceRating: falló la liquidación posterior: ${e.localizedMessage}")
                AppTelemetry.error("rating", "Falló la liquidación posterior a la calificación", e)
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

        // System message in chat (chat stays on Firebase)
        val welcomeMsg = ChatMessage(
            jobId = updatedJob.id,
            senderId = "system",
            senderName = "Sistema",
            text = "¡El profesional aceptó la solicitud de contratación directa! Chat seguro iniciado."
        )
        chatMessageDao.insertMessage(welcomeMsg)
        try {
            firebaseService.sendChatMessageToCloud(welcomeMsg)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo enviar mensaje de sistema al chat: ${e.localizedMessage}")
        }
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
        val startMsg = ChatMessage(
            jobId = updatedJob.id,
            senderId = "system",
            senderName = "Sistema",
            text = "¡El profesional inició el servicio! El trabajo está en progreso."
        )
        chatMessageDao.insertMessage(startMsg)
        try {
            firebaseService.sendChatMessageToCloud(startMsg)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo enviar mensaje de sistema al chat: ${e.localizedMessage}")
        }
        AppTelemetry.event("job.start", mapOf("jobId" to jobId))
    }

    suspend fun finishService(jobId: String, stars: Int, comment: String) = withContext(Dispatchers.IO) {
        val updatedJob = try {
            apiService.finishService(
                jobId,
                com.galibu.core.data.remote.RatingRequestDto(
                    stars = stars.coerceIn(1, 5),
                    comment = comment.ifBlank { null }
                )
            )
        } catch (e: Exception) {
            Log.e("GalibuRepository", "finishService falló en backend: ${e.localizedMessage}")
            throw e
        }
        jobRequestDao.insertJob(updatedJob)
        val finishMsg = ChatMessage(
            jobId = updatedJob.id,
            senderId = "system",
            senderName = "Sistema",
            text = "¡Servicio finalizado! Ya puedes calificar el trabajo realizado."
        )
        chatMessageDao.insertMessage(finishMsg)
        try {
            firebaseService.sendChatMessageToCloud(finishMsg)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo enviar mensaje de sistema al chat: ${e.localizedMessage}")
        }
        AppTelemetry.event("job.finish", mapOf("jobId" to jobId, "stars" to stars.toString()))
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

        // System message in chat (chat stays on Firebase)
        val senderText = if (isClient) "El cliente" else "El profesional"
        val sysMsg = ChatMessage(
            jobId = jobId,
            senderId = "system",
            senderName = "Sistema",
            text = "¡$senderText propuso una nueva tarifa de $${amount.toInt()}/h!"
        )
        chatMessageDao.insertMessage(sysMsg)
        try {
            firebaseService.sendChatMessageToCloud(sysMsg)
        } catch (e: Exception) {
            Log.e("GalibuRepository", "No se pudo enviar mensaje de sistema al chat: ${e.localizedMessage}")
        }
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


