package com.familia.monitor

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.widget.Toast
import android.os.Handler
import android.os.Looper

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
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Servicio de captura conectado y escuchando...")

        // Señal visual de que el servicio arrancó
        mainHandler.post {
            Toast.makeText(applicationContext, "Monitoreo Familiar: Escuchando...", Toast.LENGTH_SHORT).show()
        }

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

        Log.d(TAG, "Notificación detectada de: $packageName. Evaluando contenido...")

        // 1. Detectar si el remitente es desconocido (Llamada o mensaje)
        val isUnknown = ContactHelper.isContactUnknown(applicationContext, title)
        if (isUnknown && title.isNotEmpty()) {
            val category = if (sbn.notification.category == Notification.CATEGORY_CALL) "llamada_desconocida" else "mensaje_desconocido"

            Log.i(TAG, "Detección: Contacto desconocido ($title)")

            scope.launch {
                AlertUploader.sendAlert(applicationContext, packageName, category, RiskEngine.RiskLevel.MEDIUM.name, "Interacción con: $title. Texto: $text", sbn.postTime)
            }
        }

        // 2. Evaluar reglas de riesgo
        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            Log.i(TAG, "¡RIESGO DETECTADO! Categoría: ${match.category}")

            // Señal visual para el testeo
            mainHandler.post {
                Toast.makeText(applicationContext, "⚠️ RIESGO: ${match.category}", Toast.LENGTH_LONG).show()
            }

            scope.launch {
                AlertUploader.sendAlert(applicationContext, packageName, match.category, match.level.name, match.matchedFragment, sbn.postTime)
            }
        }
    }
}
