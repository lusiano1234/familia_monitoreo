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
    "com.whatsapp.w4b", // WhatsApp Business
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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "== SERVICIO CREADO ==")
        mainHandler.post {
            Toast.makeText(applicationContext, "Monitoreo: Iniciando servicio...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "== ESCUCHANDO NOTIFICACIONES ==")

        val prefs = getSharedPreferences("consent", MODE_PRIVATE)
        val consentGiven = prefs.getBoolean("consent_given", false)
        
        mainHandler.post {
            Toast.makeText(applicationContext, "Monitoreo Familiar: CONECTADO", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // LOG Y AVISO VISUAL PARA DIAGNÓSTICO (Cualquier app)
        Log.d(TAG, "Notificación detectada de: $packageName")
        mainHandler.post {
            Toast.makeText(applicationContext, "Captura: $packageName", Toast.LENGTH_SHORT).show()
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Sin Título"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "Sin Texto"
        val fullText = "$title: $text"

        if (packageName in MONITORED_PACKAGES) {
            Log.i(TAG, "PROCESANDO APP MONITOREADA: $packageName")
            
            // 1. Detección de desconocidos
            try {
                val isUnknown = ContactHelper.isContactUnknown(applicationContext, title)
                if (isUnknown && title.isNotEmpty() && title != "Sin Título") {
                    Log.w(TAG, "¡CONTACTO DESCONOCIDO! -> $title")
                    scope.launch {
                        AlertUploader.sendAlert(applicationContext, packageName, "contacto_desconocido", RiskEngine.RiskLevel.MEDIUM.name, "Remitente no en agenda: $title. Msg: $text", sbn.postTime)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ContactHelper: ${e.message}")
            }

            // 2. Motor de Riesgo
            val match = RiskEngine.evaluate(fullText)
            if (match != null) {
                Log.e(TAG, "¡RIESGO DETECTADO! Categoría: ${match.category}")
                mainHandler.post {
                    Toast.makeText(applicationContext, "🚨 ALERTA: ${match.category}", Toast.LENGTH_LONG).show()
                }
                scope.launch {
                    AlertUploader.sendAlert(applicationContext, packageName, match.category, match.level.name, match.matchedFragment, sbn.postTime)
                }
            }
        }
    }
}
