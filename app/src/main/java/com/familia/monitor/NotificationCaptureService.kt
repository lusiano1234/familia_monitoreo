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

private val APP_NAMES = mapOf(
    "com.whatsapp" to "WhatsApp",
    "com.whatsapp.w4b" to "WhatsApp Business",
    "com.instagram.android" to "Instagram",
    "org.telegram.messenger" to "Telegram",
    "com.facebook.orca" to "Messenger",
    "com.snapchat.android" to "Snapchat",
    "com.google.android.dialer" to "Teléfono",
    "com.samsung.android.dialer" to "Teléfono",
    "com.android.dialer" to "Teléfono"
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
        val appName = APP_NAMES[packageName] ?: packageName

        // --- 1. DETECCIÓN DE LLAMADAS ---
        if (notification.category == Notification.CATEGORY_CALL) {
            val caller = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Desconocido"
            val isUnknown = ContactHelper.isContactUnknown(applicationContext, caller)
            val category = if (isUnknown) "llamada_desconocida" else "llamada_entrante"
            
            processMessage(appName, caller, "[LLAMADA] El usuario está recibiendo una llamada.", sbn.postTime, category)
            return
        }

        // --- 2. DETECCIÓN DE MENSAJES (MESSAGING STYLE) ---
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

        if (messagingStyle != null) {
            val groupTitle = messagingStyle.conversationTitle?.toString()
            for (message in messagingStyle.messages) {
                val sender = message.person?.name?.toString() ?: "Desconocido"
                val text = message.text?.toString() ?: ""
                val context = if (groupTitle != null) "Grupo $groupTitle -> $sender" else sender
                
                // Detectar si el mensaje es un adjunto multimedia
                var category = "mensaje"
                val mime = message.dataMimeType
                if (mime != null) {
                    category = when {
                        mime.startsWith("image/") -> "foto_recibida"
                        mime.startsWith("audio/") -> "audio_recibido"
                        mime.startsWith("video/") -> "video_recibido"
                        else -> "archivo_recibido"
                    }
                } else if (text.contains("📷 Foto") || text.contains("🎤 Audio") || text.contains("🎥 Video")) {
                    // Fallback para etiquetas de texto
                    if (text.contains("Foto")) category = "foto_recibida"
                    if (text.contains("Audio")) category = "audio_recibido"
                    if (text.contains("Video")) category = "video_recibido"
                }

                processMessage(appName, context, text, message.timestamp, category)
            }
        } else {
            // --- 3. FALLBACK (InboxStyle / Simple) ---
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Desconocido"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            
            val finalBody = if (!bigText.isNullOrBlank() && bigText.length > text.length) bigText else text
            
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (lines != null && lines.isNotEmpty()) {
                for (line in lines) {
                    processMessage(appName, title, line.toString(), sbn.postTime, "mensaje")
                }
            } else {
                processMessage(appName, title, finalBody, sbn.postTime, "mensaje")
            }
        }
    }

    private fun processMessage(appName: String, sender: String, text: String, time: Long, initialCategory: String) {
        if (text.isBlank() && initialCategory == "mensaje") return

        // Generar un identificador único para este mensaje/evento
        val msgHash = (appName + sender + text + initialCategory).hashCode()
        
        synchronized(processedHashes) {
            if (processedHashes.contains(msgHash)) return // Ya procesado
            if (processedHashes.size > 30) {
                processedHashes.remove(processedHashes.iterator().next())
            }
            processedHashes.add(msgHash)
        }

        val fullText = "$sender: $text"
        val battery = DeviceStateHelper.getBatteryLevel(applicationContext)
        val connection = DeviceStateHelper.getConnectionType(applicationContext)

        // 1. Detección de desconocidos (Prioridad alta si es mensaje normal)
        if (initialCategory == "mensaje" || initialCategory.startsWith("llamada")) {
            val isUnknown = ContactHelper.isContactUnknown(applicationContext, sender)
            if (isUnknown && sender != "Desconocido" && !sender.startsWith("Grupo ")) {
                val cat = if (initialCategory.startsWith("llamada")) "llamada_desconocida" else "contacto_desconocido"
                upload(appName, cat, RiskEngine.RiskLevel.MEDIUM.name, fullText, time, battery, connection)
            }
        }

        // 2. Motor de Riesgo (Analizar el texto)
        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            upload(appName, match.category, match.level.name, fullText, time, battery, connection)
            mainHandler.post { Toast.makeText(applicationContext, "🚨 RIESGO: ${match.category}", Toast.LENGTH_SHORT).show() }
            return // Si es riesgo, ya lo subimos con su categoría específica
        }

        // 3. Si no es riesgo pero es un evento especial (foto, audio, etc), subirlo
        if (initialCategory != "mensaje") {
            upload(appName, initialCategory, RiskEngine.RiskLevel.LOW.name, fullText, time, battery, connection)
        }
    }

    private fun upload(appName: String, category: String, level: String, text: String, time: Long, battery: Int, connection: String) {
        scope.launch {
            AlertUploader.sendAlert(applicationContext, appName, category, level, text, time, battery, connection)
        }
    }
}
