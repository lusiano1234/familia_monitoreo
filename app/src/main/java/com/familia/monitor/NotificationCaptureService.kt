package com.familia.monitor

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Paquetes de mensajería y telefonía.
private val MONITORED_PACKAGES = setOf(
    "com.whatsapp",
    "com.instagram.android",
    "org.telegram.messenger",
    "com.facebook.orca",
    "com.snapchat.android",
    "com.google.android.dialer",
    "com.samsung.android.dialer",
    "com.android.dialer"
)

class NotificationCaptureService : NotificationListenerService() {

    private val TAG = "NotificationCapture"
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Servicio de captura conectado y escuchando...")
        // Solo procesamos si el consentimiento fue dado explícitamente.
        val prefs = getSharedPreferences("consent", MODE_PRIVATE)
        if (!prefs.getBoolean("consent_given", false)) {
            Log.w(TAG, "Consentimiento no detectado, desconectando servicio.")
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

        // 1. Detectar si el remitente es desconocido (Llamada o mensaje)
        val isUnknown = ContactHelper.isContactUnknown(applicationContext, title)
        if (isUnknown && title.isNotEmpty()) {
            val category = if (sbn.notification.category == Notification.CATEGORY_CALL) "llamada_desconocida" else "mensaje_desconocido"
            scope.launch {
                AlertUploader.sendAlert(applicationContext, packageName, category, RiskEngine.RiskLevel.MEDIUM.name, "Interacción con: $title. Texto: $text", sbn.postTime)
            }
        }

        // 2. Evaluar reglas de riesgo
        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            scope.launch {
                AlertUploader.sendAlert(applicationContext, packageName, match.category, match.level.name, match.matchedFragment, sbn.postTime)
            }
        }
    }
}
