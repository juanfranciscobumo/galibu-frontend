package com.galibu.professional.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.galibu.professional.MainActivity
import com.galibu.professional.R

/**
 * Push del sistema (bandeja de Android) para eventos del servicio del profesional
 * y movimientos de su billetera. Tocar la push abre la app.
 */
object ProPushHelper {

    private const val EVENT_CHANNEL_ID = "eventos_servicio_v2"
    private const val WALLET_CHANNEL_ID = "recargas_wallet_v1"

    fun showEventPush(title: String, body: String, context: Context, tag: String = title) {
        showPush(
            context = context,
            channelId = EVENT_CHANNEL_ID,
            channelName = "Eventos del servicio",
            channelDescription = "Ofertas aceptadas, inicios y finalizaciones de servicio",
            title = title,
            body = body,
            tag = tag,
            category = NotificationCompat.CATEGORY_EVENT
        )
    }

    fun showWalletPush(title: String, body: String, context: Context, tag: String = title) {
        showPush(
            context = context,
            channelId = WALLET_CHANNEL_ID,
            channelName = "Recargas de saldo",
            channelDescription = "Confirmaciones y errores de recargas de la billetera",
            title = title,
            body = body,
            tag = tag,
            category = NotificationCompat.CATEGORY_STATUS
        )
    }

    private fun showPush(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        title: String,
        body: String,
        tag: String,
        category: String
    ) {
        ensureChannel(context, channelId, channelName, channelDescription)

        val openIntent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            tag.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(category)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(tag.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permiso denegado: queda solo la campanita in-app.
        }
    }

    private fun ensureChannel(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(channelId) != null) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDescription
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
                setShowBadge(true)
            }
        )
    }
}
