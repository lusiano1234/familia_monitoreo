package com.familia.monitor

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Paquetes de apps de mensajería que nos interesan. Ajustar según necesidad.
private val MONITORED_PACKAGES = setOf(
    "com.whatsapp",
    "com.instagram.android",
    "org.telegram.messenger",
    "com.facebook.orca", // Messenger
    "com.snapchat.android"
)

class NotificationCaptureService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Solo procesamos si el consentimiento fue dado explícitamente.
        val prefs = getSharedPreferences("consent", MODE_PRIVATE)
        if (!prefs.getBoolean("consent_given", false)) {
            requestUnbind()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName !in MONITORED_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val fullText = "$title: $text"

        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            scope.launch {
                AlertUploader.sendAlert(
                    context = applicationContext,
                    sourceApp = packageName,
                    category = match.category,
                    level = match.level.name,
                    fragment = match.matchedFragment,
                    timestamp = sbn.postTime
                )
            }
        }
        // Si no hay match, el texto se descarta inmediatamente y no se
        // guarda ni se sube nada — esto es intencional para minimizar
        // la intrusión y el volumen de datos sensibles retenidos.
    }
}
