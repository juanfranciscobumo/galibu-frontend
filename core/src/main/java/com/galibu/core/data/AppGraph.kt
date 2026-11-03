package com.galibu.core.data

import android.app.Application
import com.galibu.core.data.firebase.FirebaseService
import com.galibu.core.data.local.AppDatabase
import com.galibu.core.data.remote.ApiClient
import com.galibu.core.data.remote.SharedPrefsTokenStorage
import com.galibu.core.data.remote.TokenStore
import com.galibu.core.data.repository.GalibuRepository

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
