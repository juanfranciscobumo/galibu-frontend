package com.galibu.core.data.firebase

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.galibu.core.data.AppGraph
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Servicio de mensajería Firebase Cloud Messaging.
 *
 * Responsabilidades:
 *  1. Capturar el token FCM y guardarlo en SharedPreferences. Si ya hay sesión,
 *     además lo registra en el backend (el token puede rotar en cualquier momento).
 *  2. Manejar mensajes push entrantes: mostrar la notificación del sistema (con
 *     toque que abre la app) y refrescar solicitudes al instante.
 *
 * El push es el canal rápido; el sondeo de 60 s sigue como respaldo por si el
 * dispositivo no tiene servicios de Google o la notificación se bloquea.
 */
class GalibuMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseService().storeFcmToken(this, token)
        // Si el token rota con la sesión iniciada, el backend debe conocerlo ya;
        // si no, los push seguirían yendo al token viejo.
        val app = applicationContext as? Application ?: return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AppGraph.repository(app).registerDeviceToken(token, "ANDROID")
                Log.i("GalibuMessaging", "Token FCM rotado y registrado en el backend")
            } catch (e: Exception) {
                Log.e("GalibuMessaging", "No se pudo registrar el token rotado: ${e.localizedMessage}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val jobId = remoteMessage.data["jobId"]

        // En primer plano el sistema no dibuja la notificación solo: la mostramos.
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Galibu"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        if (title.isNotBlank() || body.isNotBlank()) {
            showLocalNotification(title, body, jobId)
        }

        // Respaldo: al reabrir la pantalla de solicitudes también se refresca.
        if (jobId != null) {
            FirebaseService().markPendingJobRefresh(this)
        }

        // Refresco inmediato (ignora el throttle de 60 s).
        (applicationContext as? Application)?.let { app ->
            AppGraph.onPushReceived(app)
        }
    }

    private fun showLocalNotification(title: String, body: String, jobId: String?) {
        val channelId = "galibu_jobs"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Trabajos y solicitudes",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones de solicitudes y cambios de estado"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Tocar la push abre la app en la Activity principal de este módulo.
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            jobId?.let { putExtra("jobId", it) }
        }
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        NotificationManagerCompat.from(this)
            .notify(jobId?.hashCode() ?: System.currentTimeMillis().toInt(), builder.build())
    }
}
