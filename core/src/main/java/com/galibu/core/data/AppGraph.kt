package com.galibu.core.data

import android.app.Application
import android.util.Log
import com.galibu.core.data.firebase.FirebaseService
import com.galibu.core.data.local.AppDatabase
import com.galibu.core.data.model.AppNotification
import com.galibu.core.data.model.FirebaseCategory
import com.galibu.core.data.model.FirebaseDepartment
import com.galibu.core.data.remote.ApiClient
import com.galibu.core.data.remote.SharedPrefsTokenStorage
import com.galibu.core.data.remote.TokenStore
import com.galibu.core.data.repository.GalibuRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Grafo de dependencias del proceso.
 *
 * Existe un único [GalibuRepository] para toda la app, de modo que el modo
 * cliente y el modo profesional compartan la misma sesión, la misma base local
 * y los mismos flujos. Antes cada ViewModel construía su propio repositorio y
 * los dos estados podían divergir (saldo viejo, usuario nulo, notificaciones
 * duplicadas) al cambiar de modo.
 */
object AppGraph {

    @Volatile
    private var repositoryInstance: GalibuRepository? = null

    @Volatile
    private var tokenStoreInstance: TokenStore? = null

    /**
     * Alcance del proceso para el trabajo que debe existir una sola vez:
     * restaurar sesión, escuchar notificaciones y cargar configuración.
     * Antes vivía en cada ViewModel, así que al entrar a modo profesional se
     * duplicaban el colector y las cargas.
     */
    private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    private val _departments = MutableStateFlow<List<FirebaseDepartment>>(emptyList())
    val departments: StateFlow<List<FirebaseDepartment>> = _departments.asStateFlow()

    private val _categories = MutableStateFlow<List<FirebaseCategory>>(emptyList())
    val categories: StateFlow<List<FirebaseCategory>> = _categories.asStateFlow()

    /** Notificación que la app muestra como banner in-app. Única para todo el proceso. */
    val activeBannerNotification = MutableStateFlow<AppNotification?>(null)

    /**
     * Lo asigna la Activity (que sí conoce la clase de destino y el helper de
     * push). El repositorio solo invoca este callback.
     */
    @Volatile
    var eventPushHandler: ((AppNotification) -> Unit)? = null

    @Volatile
    private var globalSyncStarted = false

    /**
     * Arranca una sola vez la sincronización global del proceso. Es idempotente:
     * da igual cuántos ViewModels (cliente/profesional) llamen a [start].
     */
    fun start(application: Application) {
        if (globalSyncStarted) return
        synchronized(this) {
            if (globalSyncStarted) return
            globalSyncStarted = true

            val repo = repository(application)
            repo.onEventNotification = { notification ->
                eventPushHandler?.invoke(notification)
            }

            processScope.launch {
                try {
                    if (repo.currentUser.value == null && repo.restoreSession()) {
                        repo.refreshAll()
                    }
                } catch (e: Exception) {
                    Log.e("AppGraph", "Error restaurando sesión: ${e.localizedMessage}")
                } finally {
                    _sessionReady.value = true
                }
            }

            processScope.launch {
                var firstCollect = true
                repo.getNotificationsFlow().collect { list ->
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

            processScope.launch { loadGeoConfig(repo) }
        }
    }

    /** Recarga departamentos y categorías desde el backend. */
    fun refreshGeoConfig(application: Application) {
        processScope.launch { loadGeoConfig(repository(application)) }
    }

    /** Actualiza las categorías tras crear una nueva. */
    fun updateCategories(categories: List<FirebaseCategory>) {
        _categories.value = categories
    }

    private suspend fun loadGeoConfig(repo: GalibuRepository) {
        try {
            _departments.value = repo.getDepartments()
            _categories.value = repo.getCategories()
        } catch (e: Exception) {
            Log.e("AppGraph", "Error cargando configuración: ${e.localizedMessage}")
        }
    }

    fun tokenStore(application: Application): TokenStore =
        tokenStoreInstance ?: synchronized(this) {
            tokenStoreInstance ?: TokenStore(SharedPrefsTokenStorage(application))
                .also { tokenStoreInstance = it }
        }

    fun repository(application: Application): GalibuRepository =
        repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: build(application).also { repositoryInstance = it }
        }

    private fun build(application: Application): GalibuRepository {
        val database = AppDatabase.getDatabase(application)
        val tokenStore = tokenStore(application)
        return GalibuRepository(
            userProfileDao = database.userProfileDao(),
            jobRequestDao = database.jobRequestDao(),
            bidDao = database.bidDao(),
            chatMessageDao = database.chatMessageDao(),
            notificationDao = database.appNotificationDao(),
            firebaseService = FirebaseService(),
            apiService = ApiClient.authedService(tokenStore),
            tokenStore = tokenStore
        )
    }
}
