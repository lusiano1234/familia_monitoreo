package com.familia.monitor

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
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
    private var heartbeatRunnable: Runnable? = null

    companion object {
        var isServiceBound = false
        const val HASH_PREFS = "processed_messages"
        private const val MAX_HASHES = 300
        
        // Memoria volátil para evitar duplicados por ráfagas rápidas (Race conditions)
        private val fastCache = LinkedHashSet<String>(50)

        fun forceRebind(context: Context) {
            val componentName = ComponentName(context, NotificationCaptureService::class.java)
            val pm = context.packageManager
            try {
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                Log.d("NotificationCapture", "Watchdog: Reinicio forzado.")
            } catch (e: Exception) {
                Log.e("NotificationCapture", "Error rebind: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "== SERVICIO CREADO ==")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceBound = true
        Log.d(TAG, "== ESCUCHANDO NOTIFICACIONES ==")
        mainHandler.post {
            Toast.makeText(applicationContext, "Monitoreo Familiar: CONECTADO", Toast.LENGTH_SHORT).show()
        }

        // 1. Notificar al panel que el servicio ha iniciado
        val battery = DeviceStateHelper.getBatteryLevel(applicationContext)
        val connection = DeviceStateHelper.getConnectionType(applicationContext)
        upload(packageName, "sistema_inicio", RiskEngine.RiskLevel.NONE.name, "Servicio de captura iniciado en el dispositivo.", System.currentTimeMillis(), battery, connection)

        // 2. Iniciar Heartbeat (Señal de vida cada 30 min)
        startHeartbeat()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceBound = false
        Log.d(TAG, "== SERVICIO DESCONECTADO ==")
        stopHeartbeat()
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatRunnable = object : Runnable {
            override fun run() {
                if (isServiceBound) {
                    Log.d(TAG, "Enviando Heartbeat al panel...")
                    upload(packageName, "sistema_heartbeat", RiskEngine.RiskLevel.NONE.name, "💓 Lector de mensajes funcionando.", System.currentTimeMillis(), -1, "Heartbeat")
                    mainHandler.postDelayed(this, 30 * 60 * 1000) // Cada 30 minutos
                }
            }
        }
        mainHandler.postDelayed(heartbeatRunnable!!, 30 * 60 * 1000)
    }

    private fun stopHeartbeat() {
        heartbeatRunnable?.let { mainHandler.removeCallbacks(it) }
        heartbeatRunnable = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // AUDITORÍA INTERNA: Solo logs, sin Toasts para mantener discreción
        Log.i(TAG, ">>> Detección: $packageName")

        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        if (!devicePrefs.getBoolean("monitoring_enabled", true)) {
            Log.d(TAG, "Monitoreo pausado por el usuario.")
            return
        }

        val notification = sbn.notification
        val extras = notification.extras

        // 1. FILTRO DE RESÚMENES: Ignorar notificaciones de agrupación ("X mensajes nuevos")
        val isSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isSummary) {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                processHistory(packageName, messages, sbn.postTime)
            }
            return
        }

        if (packageName !in MONITORED_PACKAGES || !AppFilterHelper.isAppMonitored(applicationContext, packageName)) return

        // --- 2. DETECCIÓN DE LLAMADAS ---
        val isCall = notification.category == Notification.CATEGORY_CALL || 
                     packageName.contains("dialer") || packageName.contains("telecom")
        if (isCall) {
            val caller = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() 
                ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                ?: "Desconocido"
            val isUnknown = ContactHelper.isContactUnknown(applicationContext, caller)
            val cat = if (isUnknown) "llamada_desconocida" else "llamada_entrante"
            processMessage(packageName, caller, "[LLAMADA] Actividad de voz detectada.", sbn.postTime, cat)
            return
        }

        // --- 3. EXTRACCIÓN DE CONTENIDO ---
        var extracted = false
        
        // Capa A: Historial EXTRA_MESSAGES (WhatsApp suele agrupar aquí)
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            processHistory(packageName, messages, sbn.postTime)
            extracted = true
        }

        // Capa B: MessagingStyle (Extractor estándar de Android)
        if (!extracted) {
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
                extracted = true
            }
        }

        // Capa C: Fallback crudo (Respaldo final)
        if (!extracted) {
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
                
                // IMPORTANTE: WhatsApp suele incluir el historial en cada notificación.
                // Usamos 0L como fallback estable para que el hash no cambie con cada actualización.
                val time = p.getLong("time", 0L)
                
                if (text.isNotBlank()) {
                    processMessage(packageName, sender, text, time, "mensaje")
                }
            }
        }
    }

    private fun processMessage(packageName: String, sender: String, text: String, time: Long, initialCategory: String) {
        if (text.isBlank() && initialCategory == "mensaje") {
            return
        }

        // --- DEDUPLICACIÓN PRIORITARIA (HUELLA DIGITAL) ---
        val msgSignature = "sig_${(packageName + sender + text + time).hashCode()}"
        
        synchronized(fastCache) {
            if (fastCache.contains(msgSignature)) return
            if (fastCache.size > 200) fastCache.remove(fastCache.iterator().next())
            fastCache.add(msgSignature)
        }

        val prefs = getSharedPreferences(HASH_PREFS, MODE_PRIVATE)
        if (prefs.contains(msgSignature)) {
            return
        }

        // --- FILTRO DE ANTIGÜEDAD ---
        val now = System.currentTimeMillis()
        if (time > 0 && (now - time > 10 * 60 * 1000) && initialCategory == "mensaje") {
            Log.d(TAG, "MEMORIA: Saltando mensaje antiguo de $sender")
            return
        }

        // Guardar la huella en disco
        val allKeys = prefs.all.keys
        if (allKeys.size > MAX_HASHES) {
            prefs.edit().clear().apply()
        }
        prefs.edit().putBoolean(msgSignature, true).apply()

        // Guardar localmente en el registro de chats
        MessageLogHelper.saveMessage(applicationContext, APP_NAMES[packageName] ?: packageName, sender, text, if (time > 0) time else System.currentTimeMillis())

        val battery = DeviceStateHelper.getBatteryLevel(applicationContext)
        val connection = DeviceStateHelper.getConnectionType(applicationContext)
        
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        val isTotalSupervision = devicePrefs.getBoolean("total_supervision", false)

        // Actualizar timestamp de actividad
        devicePrefs.edit().putLong("last_capture_time", System.currentTimeMillis()).apply()

        val fullText = "$sender: $text"

        // 1. Motor de Riesgo
        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            Log.e(TAG, "¡RIESGO DETECTADO! Enviando: $fullText")
            mainHandler.post {
                Toast.makeText(applicationContext, "🚨 ALERTA DE RIESGO", Toast.LENGTH_SHORT).show()
            }
            upload(packageName, match.category, match.level.name, fullText, time, battery, connection)
            return
        }

        // 2. Modo de Supervisión Total (Diagnóstico Profundo)
        if (isTotalSupervision && initialCategory == "mensaje") {
            // Captura técnica de datos crudos para análisis remoto
            upload(packageName, "supervision_total", RiskEngine.RiskLevel.NONE.name, fullText, time, battery, connection)
            return
        }

        // 3. Envío de eventos especiales
        if (initialCategory != "mensaje") {
            upload(packageName, initialCategory, RiskEngine.RiskLevel.LOW.name, fullText, time, battery, connection)
            return
        }

        // 4. Detección de Desconocidos
        val isUnknown = ContactHelper.isContactUnknown(applicationContext, sender)
        if (isUnknown && sender != "Desconocido" && !sender.startsWith("Grupo ")) {
            upload(packageName, "contacto_desconocido", RiskEngine.RiskLevel.MEDIUM.name, fullText, time, battery, connection)
        }
    }

    private fun upload(appName: String, category: String, level: String, text: String, time: Long, battery: Int, connection: String) {
        val finalAppName = APP_NAMES[appName] ?: appName
        scope.launch {
            AlertUploader.sendAlert(applicationContext, finalAppName, category, level, text, time, battery, connection)
        }
    }
}
