package com.galibu.core.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galibu.core.data.local.AppDatabase
import com.galibu.core.data.firebase.FirebaseService
import com.galibu.core.data.remote.ApiClient
import com.galibu.core.data.remote.ApiService
import com.galibu.core.data.remote.SharedPrefsTokenStorage
import com.galibu.core.data.remote.TokenStore
import com.galibu.core.data.model.*
import com.galibu.core.data.repository.GalibuRepository
import com.galibu.core.ui.components.GalibuAlertType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class SharedViewModel(application: Application) : AndroidViewModel(application) {

    protected val database = AppDatabase.getDatabase(application)
    val firebaseService = FirebaseService()
    val tokenStore = TokenStore(SharedPrefsTokenStorage(application))
    val apiService: ApiService = ApiClient.authedService(tokenStore)
    
    val repository = GalibuRepository(
        userProfileDao = database.userProfileDao(),
        jobRequestDao = database.jobRequestDao(),
        bidDao = database.bidDao(),
        chatMessageDao = database.chatMessageDao(),
        notificationDao = database.appNotificationDao(),
        firebaseService = firebaseService,
        apiService = apiService,
        tokenStore = tokenStore
    )

    val currentUser: StateFlow<UserProfile?> = repository.currentUser

    fun refreshAll() {
        viewModelScope.launch {
            try {
                repository.refreshAll()
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error en refreshAll: ${e.localizedMessage}")
            }
        }
    }

    private val _sessionReady = MutableStateFlow(false)
    /** True once the session-restore attempt (Firebase Auth -> profile) has finished. */
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    private val _activeRole = MutableStateFlow<String?>(null)
    val activeRole: StateFlow<String?> = _activeRole.asStateFlow()

    val activeBannerNotification = MutableStateFlow<AppNotification?>(null)

    private val _isGlobalLoading = MutableStateFlow(false)
    val isGlobalLoading = _isGlobalLoading.asStateFlow()

    private val _globalLoadingMessage = MutableStateFlow("Cargando...")
    val globalLoadingMessage = _globalLoadingMessage.asStateFlow()

    fun showLoading(message: String = "Sincronizando con Firebase...") {
        _globalLoadingMessage.value = message
        _isGlobalLoading.value = true
    }

    fun hideLoading() {
        _isGlobalLoading.value = false
    }

    data class GlobalAlert(
        val title: String,
        val message: String,
        val type: GalibuAlertType = GalibuAlertType.INFO
    )

    private val _globalAlert = MutableStateFlow<GlobalAlert?>(null)
    val globalAlert = _globalAlert.asStateFlow()

    /**
     * Muestra una alerta con el diseño único de Galibu.
     *
     * Una alerta nunca debe convivir con el overlay de carga: si había un proceso
     * bloqueante visible, se oculta antes de mostrar el mensaje.
     *
     * [isError] se conserva por compatibilidad con el código existente. Si no se
     * especifica [type], se infiere success/error/warning/info a partir del texto.
     */
    fun showAlert(
        title: String,
        message: String,
        isError: Boolean = false,
        type: GalibuAlertType? = null
    ) {
        hideLoading()
        _globalAlert.value = GlobalAlert(
            title = title,
            message = message,
            type = type ?: inferAlertType(title, message, isError)
        )
    }

    fun showSuccess(message: String, title: String = "Aviso") =
        showAlert(title, message, type = GalibuAlertType.SUCCESS)

    fun showError(message: String, title: String = "Aviso") =
        showAlert(title, message, type = GalibuAlertType.ERROR)

    fun showWarning(message: String, title: String = "Aviso") =
        showAlert(title, message, type = GalibuAlertType.WARNING)

    fun showInfo(message: String, title: String = "Aviso") =
        showAlert(title, message, type = GalibuAlertType.INFO)

    fun dismissAlert() {
        _globalAlert.value = null
    }

    private fun inferAlertType(
        title: String,
        message: String,
        isError: Boolean
    ): GalibuAlertType {
        if (isError) return GalibuAlertType.ERROR
        val text = "$title $message".lowercase()

        val errorSignals = listOf(
            "no se pudo", "error", "falló", "fallo", "inválid", "incorrect",
            "no permitida", "no disponible", "problema", "conexión", "conexion",
            "no funciona"
        )
        if (errorSignals.any { text.contains(it) }) return GalibuAlertType.ERROR

        val successSignals = listOf(
            "exitos", "éxito", "exito", "gracias", "enviado", "enviada",
            "acept", "ofreciste", "sincronizado", "finalizad", "actualizad",
            "creada", "publicada", "calificaste", "archivad", "completado",
            "completada"
        )
        if (successSignals.any { text.contains(it) }) return GalibuAlertType.SUCCESS

        val warningSignals = listOf(
            "selecciona", "ingresa", "escribe", "permiso", "gps", "apagado",
            "desactivad", "insuficiente", "mínimo", "minimo", "muy baja",
            "no tienes", "valida", "primero", "por favor", "revisa",
            "denegad", "debes", "no puedes"
        )
        if (warningSignals.any { text.contains(it) }) return GalibuAlertType.WARNING

        return GalibuAlertType.INFO
    }

    data class PortfolioItem(val title: String, val desc: String, val imageUrl: String)
    
    val portfolios = mapOf(
        "pro_123" to listOf(
            PortfolioItem("Remodelación Baño Principal", "Instalación completa de griferías premium, tuberías PVC reforzadas de alta presión y acabados de cerámica.", "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=400"),
            PortfolioItem("Certificado Especialista Sena", "Acreditación oficial en fontanería, instalaciones de agua domiciliarias y soldadura galvánica de cobre.", "https://images.unsplash.com/photo-1589330694653-ded6df03f754?w=400"),
            PortfolioItem("Reparación Trampa de Grasas", "Limpieza técnica e instalación de filtros herméticos en restaurante céntrico de Bogotá.", "https://images.unsplash.com/photo-1542013936693-8848e5740a7a?w=400")
        ),
        "bot_luis_ferruccio" to listOf(
            PortfolioItem("Reparación Grifo Monocontrol", "Desensamble y corrección de fuga por empaque dañado en grifería de cocina de lujo.", "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=400"),
            PortfolioItem("Instalación Calentador a Gas", "Montaje seguro de calentador de paso de 5.5 litros con ducto de evacuación homologado.", "https://images.unsplash.com/photo-1621905252507-b354bc25edac?w=400")
        ),
        "bot_gerson_martnez" to listOf(
            PortfolioItem("Tableros Eléctricos Bifásicos", "Actualización completa de tacos de energía e interruptores de sobrecarga residencial.", "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=400"),
            PortfolioItem("Certificación Retie 2026", "Acreditación de normas de seguridad colombianas para instalaciones eléctricas.", "https://images.unsplash.com/photo-1510519138101-570d1dca3d66?w=400")
        ),
        "bot_gerson_martínez" to listOf(
            PortfolioItem("Tableros Eléctricos Bifásicos", "Actualización completa de tacos de energía e interruptores de sobrecarga residencial.", "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=400"),
            PortfolioItem("Certificación Retie 2026", "Acreditación de normas de seguridad colombianas para instalaciones eléctricas.", "https://images.unsplash.com/photo-1510519138101-570d1dca3d66?w=400")
        )
    )

    fun getPortfolio(proId: String): List<PortfolioItem> {
        val cleanProId = proId.trim().lowercase()
        return portfolios[cleanProId] ?: portfolios[proId] ?: listOf(
            PortfolioItem("Trabajo General Galibu", "Instalaciones, reparaciones y asesorías inmediatas de calidad.", "https://images.unsplash.com/photo-1621905252507-b354bc25edac?w=400")
        )
    }

    private val _firestoreDepartments = MutableStateFlow<List<FirebaseDepartment>>(emptyList())
    val firestoreDepartments: StateFlow<List<FirebaseDepartment>> = _firestoreDepartments.asStateFlow()

    private val _firestoreCategories = MutableStateFlow<List<FirebaseCategory>>(emptyList())
    val firestoreCategories: StateFlow<List<FirebaseCategory>> = _firestoreCategories.asStateFlow()

    init {
        // Push del sistema ante eventos del ciclo de vida (cada app lo muestra).
        repository.onEventNotification = { notif -> dispatchEventPush(notif) }
        // Persisted JWT survives app kills: restore the user and pull fresh data.
        // Only an explicit logout clears tokens, so only it ends the session.
        viewModelScope.launch {
            try {
                if (repository.restoreSession()) {
                    repository.refreshAll()
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error restaurando sesión: ${e.localizedMessage}")
            } finally {
                _sessionReady.value = true
            }
        }

        viewModelScope.launch {
            var firstCollect = true
            repository.getNotificationsFlow().collect { list ->
                if (firstCollect) {
                    firstCollect = false
                    return@collect
                }
                if (list.isNotEmpty()) {
                    val latest = list.maxByOrNull { it.timestamp }
                    if (latest != null && System.currentTimeMillis() - latest.timestamp < 15000) {
                        activeBannerNotification.value = latest
                    }
                }
            }
        }

        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    if (user.role == "CLIENT") {
                        _activeRole.value = "CLIENT"
                    } else if (user.role == "PROFESSIONAL") {
                        _activeRole.value = "PROFESSIONAL"
                    } else if (user.role == "BOTH" && _activeRole.value == null) {
                        _activeRole.value = "CLIENT"
                    }
                } else {
                    _activeRole.value = null
                }
            }
        }

        viewModelScope.launch {
            try {
                _firestoreDepartments.value = repository.getDepartments()
                _firestoreCategories.value = repository.getCategories()
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error loading dynamic config: ${e.localizedMessage}")
            }
        }
    }

    /** Recarga departamentos y categorías del backend (para pantallas que los listan). */
    fun refreshGeoConfig() {
        viewModelScope.launch {
            try {
                _firestoreDepartments.value = repository.getDepartments()
                _firestoreCategories.value = repository.getCategories()
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error recargando geo config: ${e.localizedMessage}")
            }
        }
    }

    fun addCategory(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addCategory(name)
            if (success) {
                try {
                    _firestoreCategories.value = repository.getCategories()
                } catch (e: Exception) {
                    Log.e("SharedViewModel", "Error reloading categories: ${e.localizedMessage}")
                }
            }
            onResult(success)
        }
    }

    val allJobs: StateFlow<List<JobRequest>> = repository.getAllJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val professionals: StateFlow<List<UserProfile>> = repository.getAllProfessionals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openJobs: StateFlow<List<JobRequest>> = repository.getOpenJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val jobIdsWithBids: StateFlow<List<String>> = currentUser
        .flatMapLatest { user ->
            if (user != null && (user.role == "PROFESSIONAL" || user.role == "BOTH")) {
                repository.getJobIdsWithBids(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val proBids: StateFlow<List<Bid>> = currentUser
        .flatMapLatest { user ->
            if (user != null && (user.role == "PROFESSIONAL" || user.role == "BOTH")) {
                repository.getBidsByProfessional(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.getNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedJobId = MutableStateFlow<String?>(null)
    val selectedJobId = _selectedJobId.asStateFlow()

    private val _typingStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStatus = _typingStatus.asStateFlow()

    // Completado forzoso del perfil tras Google con datos faltantes.
    private val _requireProfileCompletion = MutableStateFlow(false)
    val requireProfileCompletion = _requireProfileCompletion.asStateFlow()

    fun setRequireProfileCompletion(value: Boolean) {
        _requireProfileCompletion.value = value
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val isPartnerTyping: StateFlow<Boolean> = typingStatus
        .map { map ->
            val currentUserId = currentUser.value?.id ?: ""
            map.filterKeys { it != currentUserId }.values.any { it }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    protected var activeChatsListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCleared() {
        super.onCleared()
        activeChatsListener?.remove()
    }

    /** Todos los mensajes locales: listas de conversaciones y conteo de no leídos. */
    val allChatMessages: StateFlow<List<ChatMessage>> = repository.getAllChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Escucha en tiempo real los chats de varios jobs (listas de conversaciones).
     * Devuelve los registros para liberarlos con [stopListeningToChats].
     */
    fun listenToChats(jobIds: List<String>): List<com.google.firebase.firestore.ListenerRegistration?> {
        return jobIds.map { repository.syncChatsForJob(it) }
    }

    fun stopListeningToChats(listeners: List<com.google.firebase.firestore.ListenerRegistration?>) {
        listeners.forEach { registration ->
            try {
                registration?.remove()
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla liberar listener de chat: ${e.localizedMessage}")
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedJob: StateFlow<JobRequest?> = _selectedJobId
        .flatMapLatest { id ->
            if (id != null) repository.getJobById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedJobBids: StateFlow<List<Bid>> = _selectedJobId
        .flatMapLatest { id ->
            if (id != null) repository.getBidsForJob(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedJobMessages: StateFlow<List<ChatMessage>> = _selectedJobId
        .flatMapLatest { id ->
            if (id != null) repository.getChatMessagesForJob(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loginWithGoogle(
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
        workPhotos: List<String> = emptyList(),
        customHourlyRate: Double? = null,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                showLoading("Iniciando sesión segura con Google...")
                val success = repository.loginWithGoogle(
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
                    customWorkPhotos = workPhotos,
                    customHourlyRate = customHourlyRate,
                    context = getApplication<Application>()
                )
                if (success) {
                    _activeRole.value = role
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla inicio de sesión con Google: ${e.localizedMessage}")
                onResult(false)
            } finally {
                hideLoading()
            }
        }
    }

    fun login(email: String, password: String, role: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                showLoading("Verificando credenciales...")
                kotlinx.coroutines.delay(1000)
                val errorMsg = repository.login(email, password, role)
                if (errorMsg == null) {
                    _activeRole.value = role
                }
                onResult(errorMsg)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla inicio de sesión con contraseña: ${e.localizedMessage}")
                onResult(e.localizedMessage ?: "Error desconocido durante el inicio de sesión.")
                val warning = AppNotification(
                    userId = "",
                    title = "Error de Sesión 🔐",
                    body = "No se pudo conectar al servidor: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
            } finally {
                hideLoading()
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                showLoading("Enviando correo de recuperación...")
                kotlinx.coroutines.delay(1000)
                if (repository.isFirebaseInitialized()) {
                    repository.sendPasswordResetEmail(email)
                    onResult(null)
                } else {
                    onResult(null)
                    val notification = AppNotification(
                        userId = "",
                        title = "Correo de Recuperación (Simulado) ✉️",
                        body = "Se ha enviado un enlace para restablecer la contraseña de $email satisfactoriamente."
                    )
                    database.appNotificationDao().insertNotification(notification)
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla al enviar correo de recuperación: ${e.localizedMessage}")
                onResult(e.localizedMessage ?: "No se pudo enviar el correo de recuperación")
            } finally {
                hideLoading()
            }
        }
    }

    fun register(
        name: String,
        email: String,
        phone: String,
        password: String = "",
        role: String,
        category: String?,
        department: String? = "Bogotá D.C.",
        municipality: String? = "Bogotá",
        profilePhotoUrl: String? = null,
        address: String? = "",
        workPhotos: List<String> = emptyList(),
        hourlyRate: Double? = null,
        acceptedTerms: Boolean = false,
        googleIdToken: String? = null,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            showLoading("Creando nueva cuenta de usuario...")
            kotlinx.coroutines.delay(1200)
            try {
                repository.registerUser(
                    getApplication<Application>(),
                    name, email, phone, password, role, category,
                    department, municipality, profilePhotoUrl, address,
                    workPhotos, hourlyRate, acceptedTerms, googleIdToken
                )
                onResult(null)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla de registro: ${e.localizedMessage}")
                val rawMsg = e.localizedMessage ?: ""
                val friendlyMessage = if (rawMsg.contains("EMAIL_EXISTS", ignoreCase = true) ||
                        rawMsg.contains("ya está registrado", ignoreCase = true)) {
                    "Este correo ya está registrado. Por favor, inicia sesión."
                } else {
                    rawMsg.ifBlank { "Error de comunicación con el servidor" }
                }
                onResult(friendlyMessage)
            } finally {
                hideLoading()
            }
        }
    }

    fun updateProfile(name: String, phone: String = "", workCategory: String?, profilePhotoUrl: String?, department: String? = "Bogotá D.C.", municipality: String? = "Bogotá", address: String? = null, hourlyRate: Double? = null, workPhotos: List<String>? = null, workDescription: String? = null) {
        viewModelScope.launch {
            try {
                showLoading("Actualizando información de tu perfil...")
                kotlinx.coroutines.delay(1000)

                // Teléfono sin UI: en blanco conserva el número actual.
                val finalPhone = phone.ifBlank { currentUser.value?.phone ?: "" }                
                var finalPhotoUrl = profilePhotoUrl
                val currentUserId = currentUser.value?.id ?: "unknown"
                if (!profilePhotoUrl.isNullOrEmpty() && !profilePhotoUrl.startsWith("http") && !profilePhotoUrl.startsWith("data:image")) {
                    try {
                        val bytes = if (profilePhotoUrl.startsWith("content://") || profilePhotoUrl.startsWith("file://")) {
                            val uri = profilePhotoUrl.toUri()
                            getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } else {
                            val file = java.io.File(profilePhotoUrl)
                            if (file.exists()) file.readBytes() else null
                        }
                        if (bytes != null) {
                            val uploadedUrl = firebaseService.uploadProfilePhoto(currentUserId, bytes)
                            finalPhotoUrl = uploadedUrl
                        }
                    } catch (e: Exception) {
                        Log.e("SharedViewModel", "Error reading/uploading chosen image: ${e.localizedMessage}")
                    }
                }

                // Sube las fotos nuevas del portafolio y conserva las que ya son URL.
                val finalWorkPhotos = workPhotos?.let { photos ->
                    uploadPortfolioPhotos(currentUserId, photos)
                }
                
                repository.updateProfile(
                    name = name,
                    phone = finalPhone,
                    workCategory = workCategory,
                    profilePhotoUrl = finalPhotoUrl,
                    department = department,
                    municipality = municipality,
                    address = address,
                    hourlyRate = hourlyRate,
                    workPhotos = finalWorkPhotos?.joinToString("|"),
                    workDescription = workDescription
                )
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla actualización de perfil: ${e.localizedMessage}")
                val user = currentUser.value
                val warning = AppNotification(
                    userId = user?.id ?: "",
                    title = "Perfil Actualizado Localmente ⏳",
                    body = "Tu perfil se guardó de forma local en el dispositivo. Falló sincronización con Firebase Firestore: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Sube las fotos nuevas del portafolio (data URI, content:// o file://) y
     * conserva las que ya son URL. Si una subida falla, no se pierde la foto:
     * se guarda el valor original.
     */
    private suspend fun uploadPortfolioPhotos(userId: String, photos: List<String>): List<String> {
        val uploaded = mutableListOf<String>()
        photos.forEachIndexed { index, photo ->
            val value = photo.trim()
            if (value.isBlank()) return@forEachIndexed
            if (value.startsWith("http://") || value.startsWith("https://")) {
                uploaded.add(value)
                return@forEachIndexed
            }

            val bytes = try {
                when {
                    value.startsWith("data:image") -> {
                        val comma = value.indexOf(',')
                        if (comma == -1) {
                            null
                        } else {
                            android.util.Base64.decode(
                                value.substring(comma + 1),
                                android.util.Base64.DEFAULT
                            )
                        }
                    }
                    value.startsWith("content://") || value.startsWith("file://") -> {
                        getApplication<Application>().contentResolver
                            .openInputStream(value.toUri())
                            ?.use { it.readBytes() }
                    }
                    else -> {
                        val file = java.io.File(value)
                        if (file.exists()) file.readBytes() else null
                    }
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "No se pudo leer la foto ${index + 1} del portafolio: ${e.localizedMessage}")
                null
            }

            if (bytes == null) {
                uploaded.add(value)
                return@forEachIndexed
            }

            val stored = try {
                firebaseService.uploadWorkPhoto(
                    userId = userId,
                    photoId = "${index}_${System.currentTimeMillis()}",
                    photoBytes = bytes
                )
            } catch (e: Exception) {
                Log.e("SharedViewModel", "No se pudo subir la foto ${index + 1} del portafolio: ${e.localizedMessage}")
                value
            }
            uploaded.add(stored)
        }
        return uploaded
    }

    fun toggleUserRole() {
        viewModelScope.launch {
            try {
                val user = currentUser.value ?: return@launch
                val targetRole = if (user.role == "CLIENT") {
                    "PROFESSIONAL"
                } else if (user.role == "PROFESSIONAL") {
                    "CLIENT"
                } else {
                    if (_activeRole.value == "CLIENT") "PROFESSIONAL" else "CLIENT"
                }
                if (user.role == "BOTH") {
                    _activeRole.value = targetRole
                    showLoading("Cambiando vista de rol...")
                    kotlinx.coroutines.delay(800)
                    return@launch
                }
                val updatedProfile = user.copy(role = targetRole)
                showLoading("Cambiando vista de rol...")
                kotlinx.coroutines.delay(800)
                repository.updateUserDirectly(updatedProfile)
                _activeRole.value = targetRole
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla al cambiar de rol: ${e.localizedMessage}")
            } finally {
                hideLoading()
            }
        }
    }

    fun updateUserDirectly(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateUserDirectly(profile)
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                showLoading("Actualizando contraseña segura...")
                kotlinx.coroutines.delay(1000)
                onResult(repository.changePassword(currentPassword, newPassword))
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla cambio de contraseña: ${e.localizedMessage}")
                onResult(e.localizedMessage ?: "No se pudo cambiar la contraseña.")
            } finally {
                hideLoading()
            }
        }
    }

    fun deleteAccount(onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                showLoading("Eliminando tu cuenta de forma segura...")
                kotlinx.coroutines.delay(1500)
                val error = repository.deleteAccount()
                if (error == null) {
                    _selectedJobId.value = null
                }
                onResult(error)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla eliminación de cuenta: ${e.localizedMessage}")
                onResult(e.localizedMessage ?: "No se pudo eliminar la cuenta.")
            } finally {
                hideLoading()
            }
        }
    }

    fun cancelJobRequest(jobId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                showLoading("Cancelando solicitud...")
                repository.cancelJobRequest(jobId)
                onResult(true)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla cancelando solicitud: ${e.localizedMessage}")
                onResult(false)
            } finally {
                hideLoading()
            }
        }
    }

    fun rechargeBalance(amount: Double) {
        viewModelScope.launch {
            try {
                showLoading("Procesando recarga segura PSE/Tarjeta...")
                kotlinx.coroutines.delay(1200)
                repository.rechargeBalance(amount)
            } catch (e: Exception) {
                hideLoading()
                Log.e("SharedViewModel", "Falla recarga: ${e.localizedMessage}")
                showAlert("Aviso", "No se pudo procesar la recarga. Verifica tu conexión e inténtalo de nuevo.", true)
            } finally {
                hideLoading()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                showLoading("Cerrando tu sesión de forma segura...")
                kotlinx.coroutines.delay(1000)
                repository.logout()
                _selectedJobId.value = null
                _activeRole.value = null
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla al cerrar sesión: ${e.localizedMessage}")
                showError("No se pudo cerrar la sesión. Verifica tu conexión e inténtalo nuevamente.")
            } finally {
                hideLoading()
            }
        }
    }

    // --- Job Selection ---

    /** Push del sistema para eventos del ciclo de vida. Cada app lo implementa. */
    protected open fun dispatchEventPush(notif: AppNotification) {}

    /** Push del sistema para movimientos de la billetera. Por defecto usa el canal de eventos. */
    protected open fun dispatchWalletPush(notif: AppNotification) = dispatchEventPush(notif)

    /**
     * Guarda una notificación en la campanita local y lanza el push del sistema.
     * [wallet] permite usar un canal de notificación específico para recargas.
     */
    protected fun publishUserNotification(
        title: String,
        body: String,
        jobId: String? = null,
        wallet: Boolean = false
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val notification = AppNotification(
                    userId = user.id,
                    title = title,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    jobId = jobId
                )
                database.appNotificationDao().insertNotification(notification)
                if (wallet) {
                    dispatchWalletPush(notification)
                } else {
                    dispatchEventPush(notification)
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla publicando notificación: ${e.localizedMessage}")
            }
        }
    }

    /** Baja los jobs del backend (dispara detección de eventos aceptar/iniciar/finalizar). */
    fun refreshMyJobs() {
        viewModelScope.launch {
            try {
                repository.refreshJobs()
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla refrescar jobs: ${e.localizedMessage}")
            }
        }
    }

    fun selectJob(jobId: String?) {
        activeChatsListener?.remove()
        activeChatsListener = null

        _selectedJobId.value = jobId

        if (jobId != null) {
            activeChatsListener = repository.syncChatsForJob(jobId)
            viewModelScope.launch {
                try {
                    repository.refreshJobDetails(jobId)
                } catch (e: Exception) {
                    Log.e("SharedViewModel", "Error refrescando detalle: ${e.localizedMessage}")
                }
            }
        }
    }

    /** Re-baja el detalle del job seleccionado (precio/contraofertas) sin tocar listeners. */
    fun refreshSelectedJob() {
        val jobId = _selectedJobId.value ?: return
        viewModelScope.launch {
            try {
                repository.refreshJobDetails(jobId)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error refrescando detalle: ${e.localizedMessage}")
            }
        }
    }

    // --- Chat ---

    val messages: StateFlow<List<ChatMessage>> = selectedJobMessages

    private var typingJob: kotlinx.coroutines.Job? = null

    fun startListening(jobId: String) {
        selectJob(jobId)
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            repository.listenTypingStatus(jobId).collect {
                _typingStatus.value = it
            }
        }
        viewModelScope.launch {
            repository.markAsRead(jobId)
        }
    }

    fun stopListening() {
        val jobId = _selectedJobId.value
        if (jobId != null) {
            viewModelScope.launch {
                repository.setTypingStatus(jobId, false)
            }
        }
        typingJob?.cancel()
        typingJob = null
        _typingStatus.value = emptyMap()
        selectJob(null)
    }

    fun setTyping(typing: Boolean) {
        val jobId = _selectedJobId.value ?: return
        viewModelScope.launch {
            repository.setTypingStatus(jobId, typing)
        }
    }

    fun sendChatMessage(text: String) {
        sendMessage(text)
    }

    fun sendMessage(text: String) {
        val jobId = _selectedJobId.value ?: return
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            try {
                repository.sendChatMessage(jobId, text)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla enviar mensaje: ${e.localizedMessage}")
                val user = currentUser.value
                val warning = AppNotification(
                    userId = user?.id ?: "",
                    title = "Mensaje guardado localmente 💬",
                    body = "Tu mensaje se envió localmente. Falló sincronización con Firestore: ${e.localizedMessage}"
                )
                database.appNotificationDao().insertNotification(warning)
            }
        }
    }

    fun sendMessage(message: ChatMessage) {
        val jobId = _selectedJobId.value ?: return
        viewModelScope.launch {
            try {
                repository.sendMessage(jobId, message)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla enviar mensaje: ${e.localizedMessage}")
            }
        }
    }

    fun sendImageMessage(imageBytes: ByteArray) {
        val jobId = _selectedJobId.value ?: return
        viewModelScope.launch {
            try {
                showLoading("Enviando imagen...")
                repository.sendImageMessage(jobId, imageBytes)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla enviar imagen: ${e.localizedMessage}")
            } finally {
                hideLoading()
            }
        }
    }

    // --- Payments ---

    fun payAndCompleteJob(jobId: String, paymentMethod: String, onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                showLoading("Iniciando cierre y calificaciones...")
                kotlinx.coroutines.delay(1000)
                repository.startJobCompletion(jobId)
                onResult()
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla iniciar cierre servicio: ${e.localizedMessage}")
                onResult()
            } finally {
                hideLoading()
            }
        }
    }

    /** jobId -> timestamp del último envío de calificación (para deduplicar doble clic). */
    private val ratingLastSent = mutableMapOf<String, Long>()
    private val RATING_COOLDOWN_MS = 6_000L

    fun submitRating(
        jobId: String,
        rating: Float,
        review: String,
        isClientRating: Boolean,
        onSuccess: (() -> Unit)? = null
    ) {
        val now = System.currentTimeMillis()
        val last = ratingLastSent[jobId] ?: 0L
        if (now - last < RATING_COOLDOWN_MS) {
            Log.d("SharedViewModel", "submitRating ignorado (cooldown) para jobId=$jobId")
            return
        }
        ratingLastSent[jobId] = now
        Log.d("SharedViewModel", "submitRating iniciado jobId=$jobId stars=$rating isClient=$isClientRating")
        viewModelScope.launch {
            try {
                showLoading("Enviando calificación...")
                repository.submitServiceRating(jobId, rating, review, isClientRating)
                if (onSuccess != null) onSuccess() else showSuccess("Calificación publicada.")
                Log.d("SharedViewModel", "submitRating OK jobId=$jobId")
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla calificar servicio: ${e.localizedMessage}")
                showError("No se pudo enviar la calificación. Verifica tu conexión e inténtalo nuevamente.")
            } finally {
                // Libera el cooldown tras un breve margen para evitar reenvío inmediato.
                viewModelScope.launch {
                    delay(RATING_COOLDOWN_MS)
                    ratingLastSent.remove(jobId)
                }
                hideLoading()
            }
        }
    }

    // --- Notifications ---

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            try {
                repository.markNotificationAsRead(id)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla marcar notificación: ${e.localizedMessage}")
            }
        }
    }

    /** Marca como leídas todas las notificaciones de una solicitud (al abrir su detalle). */
    fun markJobNotificationsAsRead(jobId: String) {
        viewModelScope.launch {
            try {
                val userId = currentUser.value?.id ?: return@launch
                repository.markJobNotificationsAsRead(userId, jobId)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Falla marcar notificaciones del job: ${e.localizedMessage}")
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                val currentNotifications = notifications.value
                currentNotifications.forEach { notif ->
                    if (!notif.isRead) {
                        repository.markNotificationAsRead(notif.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error marking all notifications read: ${e.localizedMessage}")
            }
        }
    }

    // --- Firebase Sync Controls ---

    val isCloudSyncEnabled: StateFlow<Boolean> = firebaseService.isCloudSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleCloudSync(enabled: Boolean) {
        firebaseService.toggleCloudSync(enabled)
    }

    fun triggerSimulatedFcmPush(title: String, body: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val notification = AppNotification(
                userId = user.id,
                title = title,
                body = body,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            database.appNotificationDao().insertNotification(notification)
        }
    }

    fun rechargeBalanceManually(amount: Double) {
        viewModelScope.launch {
            try {
                showLoading("Procesando recarga...")
                kotlinx.coroutines.delay(1000)
                repository.rechargeBalance(amount)
                hideLoading()
            } catch (e: Exception) {
                hideLoading()
                Log.e("SharedViewModel", "Error en recarga: ${e.localizedMessage}")
            }
        }
    }
}

