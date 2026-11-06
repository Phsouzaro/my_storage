package com.mystorage.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mystorage.MainActivity
import com.mystorage.R
import com.mystorage.domain.model.AlertLevel
import com.mystorage.domain.model.ExpiryAlert
import com.mystorage.ui.components.expiryText
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpiryNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "expiry_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_LINES = 6
    }

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de validade",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Avisa quando produtos do estoque estão perto de vencer" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Uma única notificação agrupada. Retorna false se não havia permissão. */
    fun notify(alerts: List<ExpiryAlert>, today: LocalDate = LocalDate.now()): Boolean {
        if (alerts.isEmpty() || !canNotify()) return false

        val expired = alerts.count { it.level == AlertLevel.EXPIRED }
        val expiring = alerts.size - expired
        val title = listOfNotNull(
            expiring.takeIf { it > 0 }?.let { if (it == 1) "1 item vencendo em breve" else "$it itens vencendo em breve" },
            expired.takeIf { it > 0 }?.let { if (it == 1) "1 vencido" else "$it vencidos" },
        ).joinToString(" · ")

        val lines = alerts.map { "${it.lot.productName} — ${expiryText(it.lot.expiryDate, today)}" }
        val style = NotificationCompat.InboxStyle().also { s ->
            lines.take(MAX_LINES).forEach(s::addLine)
            if (lines.size > MAX_LINES) s.setSummaryText("+${lines.size - MAX_LINES} itens")
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_expiry)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setNumber(alerts.size)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            return false // permissão revogada entre a checagem e o envio
        }
        return true
    }
}
