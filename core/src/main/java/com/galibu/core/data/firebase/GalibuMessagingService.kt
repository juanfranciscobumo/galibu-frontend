package com.galibu.core.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Servicio de mensajería Firebase Cloud Messaging.
 *
 * Responsabilidades:
 *  1. Capturar el token FCM y guardarlo en SharedPreferences (para registrarlo
 *     en el backend desde el ViewModel tras iniciar sesión).
 *  2. Manejar mensajes push entrantes (mostrar notificación local y marcar
 *     que se debe refrescar jobs al reanudar la app).
 *
 * Notificación: Para que las push lleguen en producción, se debe configurar
 * Firebase Admin SDK en el backend (Render) con las credenciales del proyecto.
 * Mientras tanto, el polling largo de respaldo mantiene la app sincronizada.
 */
class GalibuMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseService().storeFcmToken(this, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Mostrar notificación local si hay contenido de notificación
        remoteMessage.notification?.let { notification ->
            showLocalNotification(
                title = notification.title ?: "Galibu",
                body = notification.body ?: "",
                jobId = remoteMessage.data["jobId"]
            )
        }

        // Marcar que hay un refresh pendiente de jobs (se procesa al reanudar la app)
        if (remoteMessage.data.containsKey("jobId")) {
            FirebaseService().markPendingJobRefresh(this)
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

        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(hashCode().toInt(), builder.build())
    }
}
