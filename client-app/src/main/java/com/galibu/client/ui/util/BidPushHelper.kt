package com.galibu.client.ui.util

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
import com.galibu.client.MainActivity
import com.galibu.client.R

/**
 * Push del sistema (bandeja de Android) para nuevas ofertas recibidas.
 * Tocar la push abre la app directo en el detalle de la solicitud.
 */
object BidPushHelper {

    const val EXTRA_OPEN_JOB_ID = "open_job_id"
    private const val CHANNEL_ID = "avisos_galibu_v2"
    private const val PREFS = "bid_push_prefs"
    private const val KEY_NOTIFIED_BID_IDS = "notified_bid_ids"

    fun isAlreadyNotified(context: Context, bidId: String): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_NOTIFIED_BID_IDS, emptySet())
            ?.contains(bidId) == true
    }

    fun notifiedBidIds(context: Context): MutableSet<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_NOTIFIED_BID_IDS, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
    }

    fun markBidNotified(context: Context, bidId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = (prefs.getStringSet(KEY_NOTIFIED_BID_IDS, emptySet()) ?: emptySet()) + bidId
        prefs.edit().putStringSet(KEY_NOTIFIED_BID_IDS, updated).apply()
    }

    fun showNewOfferPush(context: Context, title: String, body: String, jobId: String) {
        showEventPush(context, title, body, jobId, MainActivity::class.java, CHANNEL_ID)
    }

    /**
     * Push genérica de eventos del ciclo de vida (aceptar/iniciar/finalizar/calificar).
     * Tocar abre la app en el detalle de la solicitud.
     */
    fun showEventPush(
        context: Context,
        title: String,
        body: String,
        jobId: String?,
        target: Class<*>,
        channelId: String = CHANNEL_ID
    ) {
        ensureChannel(context, channelId)
        val openIntent = Intent(context, target).apply {
            if (!jobId.isNullOrBlank()) putExtra(EXTRA_OPEN_JOB_ID, jobId)
        }
        val pending = PendingIntent.getActivity(
            context,
            (jobId ?: title).hashCode(),
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
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((jobId ?: title).hashCode(), notification)
        } catch (e: SecurityException) {
            // Permiso de notificaciones denegado: queda solo la campanita in-app.
        }
    }

    private fun ensureChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (manager.getNotificationChannel(channelId) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Avisos de Galibu",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Ofertas y cambios en tus solicitudes"
                    setSound(soundUri, audioAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 200, 250)
                    setShowBadge(true)
                }
            )
        }
    }
}
