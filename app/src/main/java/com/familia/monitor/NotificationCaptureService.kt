package com.familia.monitor

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
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

    // Prevención de duplicados: guarda el hash de los últimos mensajes procesados
    private val processedHashes = LinkedHashSet<Int>(25)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "== SERVICIO CREADO ==")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "== ESCUCHANDO NOTIFICACIONES ==")
        mainHandler.post {
            Toast.makeText(applicationContext, "Monitoreo Familiar: CONECTADO", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName !in MONITORED_PACKAGES) return

        val notification = sbn.notification
        val extras = notification.extras

        // Intentar extraer usando MessagingStyle (el estándar más completo)
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

        if (messagingStyle != null) {
            val groupTitle = messagingStyle.conversationTitle?.toString()
            for (message in messagingStyle.messages) {
                val sender = message.person?.name?.toString() ?: "Desconocido"
                val text = message.text?.toString() ?: ""
                val context = if (groupTitle != null) "Grupo $groupTitle -> $sender" else sender
                
                processMessage(packageName, context, text, message.timestamp)
            }
        } else {
            // Fallback para notificaciones simples o antiguas
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Desconocido"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            
            val finalBody = if (!bigText.isNullOrBlank() && bigText.length > text.length) bigText else text
            
            // Si tiene líneas múltiples (InboxStyle, común en Telegram o resúmenes)
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (lines != null && lines.isNotEmpty()) {
                for (line in lines) {
                    processMessage(packageName, title, line.toString(), sbn.postTime)
                }
            } else {
                processMessage(packageName, title, finalBody, sbn.postTime)
            }
        }
    }

    private fun processMessage(packageName: String, sender: String, text: String, time: Long) {
        if (text.isBlank()) return

        // Generar un identificador único para este mensaje
        val msgHash = (packageName + sender + text).hashCode()
        
        synchronized(processedHashes) {
            if (processedHashes.contains(msgHash)) return // Ya procesado
            
            // Mantener solo los últimos 20 hashes para no consumir memoria infinita
            if (processedHashes.size > 20) {
                processedHashes.remove(processedHashes.iterator().next())
            }
            processedHashes.add(msgHash)
        }

        val fullText = "$sender: $text"
        Log.d(TAG, "Evaluando: $fullText")

        // Obtener estado del dispositivo para adjuntar a la alerta
        val battery = DeviceStateHelper.getBatteryLevel(applicationContext)
        val connection = DeviceStateHelper.getConnectionType(applicationContext)

        // 1. Detección de desconocidos
        try {
            val isUnknown = ContactHelper.isContactUnknown(applicationContext, sender)
            if (isUnknown && sender.isNotEmpty() && sender != "Desconocido") {
                Log.w(TAG, "¡CONTACTO DESCONOCIDO! -> $sender")
                scope.launch {
                    AlertUploader.sendAlert(
                        context = applicationContext, 
                        sourceApp = packageName, 
                        category = "contacto_desconocido", 
                        level = RiskEngine.RiskLevel.MEDIUM.name, 
                        fragment = "Remitente no en agenda: $sender. Mensaje: $text", 
                        timestamp = time,
                        battery = battery,
                        connection = connection
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ContactHelper: ${e.message}")
        }

        // 2. Motor de Riesgo
        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            Log.e(TAG, "¡RIESGO DETECTADO! [$packageName] -> $fullText")
            mainHandler.post {
                Toast.makeText(applicationContext, "🚨 ALERTA DE RIESGO", Toast.LENGTH_SHORT).show()
            }
            scope.launch {
                AlertUploader.sendAlert(
                    context = applicationContext,
                    sourceApp = packageName, 
                    category = match.category, 
                    level = match.level.name, 
                    fragment = fullText, 
                    timestamp = time,
                    battery = battery,
                    connection = connection
                )
            }
        }
    }
}
