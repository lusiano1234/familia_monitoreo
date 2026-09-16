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
    "com.whatsapp.w4b",
    "com.instagram.android",
    "org.telegram.messenger",
    "com.facebook.orca",
    "com.snapchat.android",
    "com.google.android.dialer",
    "com.samsung.android.dialer",
    "com.android.dialer",
    "com.android.server.telecom",
    "com.google.android.apps.messaging",
    "com.samsung.android.messaging",
    "com.android.mms",
    "com.zhiliaoapp.musically", // TikTok
    "com.ss.android.ugc.trill", // TikTok (otra variante)
    "com.discord", // Discord
    "com.twitter.android", // X (Twitter)
    "com.facebook.katana", // Facebook
    "com.facebook.lite", // Facebook Lite
    "com.google.android.youtube", // YouTube
    "tv.twitch.android.app" // Twitch
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
    "com.android.dialer" to "Teléfono",
    "com.android.server.telecom" to "Llamada de Sistema",
    "com.google.android.apps.messaging" to "Mensaje SMS",
    "com.samsung.android.messaging" to "Mensaje SMS",
    "com.android.mms" to "Mensaje SMS",
    "com.zhiliaoapp.musically" to "TikTok",
    "com.ss.android.ugc.trill" to "TikTok",
    "com.discord" to "Discord",
    "com.twitter.android" to "X (Twitter)",
    "com.facebook.katana" to "Facebook",
    "com.facebook.lite" to "Facebook Lite",
    "com.google.android.youtube" to "YouTube",
    "tv.twitch.android.app" to "Twitch"
)

class NotificationCaptureService : NotificationListenerService() {

    private val TAG = "NotificationCapture"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Prevención de duplicados: guarda el hash de los últimos mensajes procesados
    private val processedHashes = LinkedHashSet<Int>(50)

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
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        if (!devicePrefs.getBoolean("monitoring_enabled", true)) {
            Log.d(TAG, "Monitoreo desactivado.")
            return
        }

        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras
        val appName = APP_NAMES[packageName] ?: packageName

        // 1. FILTRO DE RESÚMENES: Ignorar la notificación "madre" que agrupa chats
        val isSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isSummary) {
            Log.d(TAG, "AUDITORÍA: Saltando notificación de resumen de $appName")
            
            // Intentar extraer si el resumen trae mensajes reales
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                processHistory(packageName, messages, sbn.postTime)
            }
            return
        }

        if (packageName !in MONITORED_PACKAGES) return

        Log.d(TAG, ">>> PROCESANDO NOTIFICACIÓN INDIVIDUAL: $appName")

        // --- 1. DETECCIÓN DE LLAMADAS ---
        val isCall = notification.category == Notification.CATEGORY_CALL || 
                     packageName.contains("dialer") || packageName.contains("telecom")
        if (isCall) {
            val caller = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() 
                ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                ?: "Desconocido"
            val isUnknown = ContactHelper.isContactUnknown(applicationContext, caller)
            val cat = if (isUnknown) "llamada_desconocida" else "llamada_entrante"
            processMessage(packageName, caller, "[LLAMADA] Actividad detectada.", sbn.postTime, cat)
            return
        }

        // --- 2. EXTRACCIÓN PROFUNDA ---
        
        // Historial EXTRA_MESSAGES
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            processHistory(packageName, messages, sbn.postTime)
        }

        // MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null) {
            val groupTitle = messagingStyle.conversationTitle?.toString()
            for (message in messagingStyle.messages) {
                val sender = message.person?.name?.toString() ?: "Desconocido"
                val text = message.text?.toString() ?: ""
                val context = if (groupTitle != null) "Grupo $groupTitle -> $sender" else sender
                
                var mediaCat = "mensaje"
                val mime = message.dataMimeType
                if (mime != null) {
                    mediaCat = when {
                        mime.startsWith("image/") -> "foto_recibida"
                        mime.startsWith("audio/") -> "audio_recibido"
                        mime.startsWith("video/") -> "video_recibido"
                        else -> "archivo_recibido"
                    }
                }
                processMessage(packageName, context, text, message.timestamp, mediaCat)
            }
        } else {
            // Fallback crudo
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Desconocido"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            
            val possibleTexts = listOf(bigText, text, subText).filter { !it.isNullOrBlank() }.distinct()
            for (t in possibleTexts) {
                val lower = t!!.lowercase()
                if (lower.contains("nuevos mensajes") || lower.contains("chats")) continue
                processMessage(packageName, title, t, sbn.postTime, "mensaje")
            }
        }
    }

    private fun processHistory(packageName: String, messages: Array<android.os.Parcelable>, postTime: Long) {
        for (p in messages) {
            if (p is android.os.Bundle) {
                val text = p.getCharSequence("text")?.toString() ?: ""
                val sender = p.getCharSequence("sender")?.toString() 
                    ?: p.getBundle("person")?.getCharSequence("name")?.toString()
                    ?: "Desconocido"
                val time = p.getLong("time", postTime)
                if (text.isNotBlank()) {
                    processMessage(packageName, sender, text, time, "mensaje")
                }
            }
        }
    }

    private fun processMessage(packageName: String, sender: String, text: String, time: Long, initialCategory: String) {
        if (text.isBlank() && initialCategory == "mensaje") return

        val fullText = "$sender: $text"
        val battery = DeviceStateHelper.getBatteryLevel(applicationContext)
        val connection = DeviceStateHelper.getConnectionType(applicationContext)

        // Motor de Riesgo (Siempre enviar si es riesgo)
        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            upload(packageName, match.category, match.level.name, fullText, time, battery, connection)
            return
        }

        // Deduplicación por hash (contenido + tiempo en segundos)
        val timeSec = time / 1000
        val msgHash = (packageName + sender + text + initialCategory + timeSec).hashCode()
        
        synchronized(processedHashes) {
            if (processedHashes.contains(msgHash)) return
            if (processedHashes.size > 200) processedHashes.remove(processedHashes.iterator().next())
            processedHashes.add(msgHash)
        }

        // Enviar si es evento especial o desconocido
        if (initialCategory != "mensaje") {
            upload(packageName, initialCategory, RiskEngine.RiskLevel.LOW.name, fullText, time, battery, connection)
        } else {
            val isUnknown = ContactHelper.isContactUnknown(applicationContext, sender)
            if (isUnknown && sender != "Desconocido" && !sender.startsWith("Grupo ")) {
                upload(packageName, "contacto_desconocido", RiskEngine.RiskLevel.MEDIUM.name, fullText, time, battery, connection)
            }
        }
    }

    private fun upload(appName: String, category: String, level: String, text: String, time: Long, battery: Int, connection: String) {
        val finalAppName = APP_NAMES[appName] ?: appName
        scope.launch {
            AlertUploader.sendAlert(applicationContext, finalAppName, category, level, text, time, battery, connection)
        }
    }
}
