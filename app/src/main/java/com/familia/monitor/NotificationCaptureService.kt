package com.familia.monitor

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Paquetes de apps de mensajería y telefonía que nos interesan.
private val MONITORED_PACKAGES = setOf(
    "com.whatsapp",
    "com.instagram.android",
    "org.telegram.messenger",
    "com.facebook.orca", // Messenger
    "com.snapchat.android",
    "com.google.android.dialer",
    "com.samsung.android.dialer",
    "com.android.dialer",
    "com.android.server.telecom"
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
        
        // Log para ver qué notificaciones llegan (útil para debug)
        if (packageName in MONITORED_PACKAGES) {
            Log.d(TAG, "Notificación recibida de app monitoreada: $packageName")
        }

        if (packageName !in MONITORED_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val fullText = "$title: $text"

        Log.d(TAG, "Evaluando texto: $fullText")

        // 1. Verificar si el remitente es un contacto desconocido
        // En la mayoría de las apps de mensajería, el título es el nombre del contacto.
        val isUnknown = ContactHelper.isContactUnknown(applicationContext, title)
        if (isUnknown && title.isNotEmpty()) {
            Log.i(TAG, "Interacción con CONTACTO DESCONOCIDO detectada: $title")
            val category = if (sbn.notification.category == Notification.CATEGORY_CALL) {
                "llamada_desconocida"
            } else {
                "mensaje_desconocido"
            }
            
            scope.launch {
                AlertUploader.sendAlert(
                    context = applicationContext,
                    sourceApp = packageName,
                    category = category,
                    level = RiskEngine.RiskLevel.MEDIUM.name,
                    fragment = "Interacción con: $title. Mensaje: $text",
                    timestamp = sbn.postTime
                )
            }
        }

        // 2. Evaluar el contenido del mensaje con el motor de riesgo
        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            Log.i(TAG, "¡RIESGO DETECTADO! Categoría: ${match.category}. Iniciando subida...")
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
        } else {
            Log.d(TAG, "No se detectaron riesgos en el contenido de la notificación.")
        }
    }
}
