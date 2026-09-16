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
    "com.android.mms"
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
    "com.android.mms" to "Mensaje SMS"
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
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        if (!devicePrefs.getBoolean("monitoring_enabled", true)) {
            Log.d(TAG, "Monitoreo desactivado.")
            return
        }

        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras
        val appName = APP_NAMES[packageName] ?: packageName

        // AUDITORÍA: Registrar cada notificación que llega de apps monitoreadas
        if (packageName in MONITORED_PACKAGES) {
            Log.d(TAG, ">>> LLEGÓ NOTIFICACIÓN DE: $appName ($packageName)")
        } else {
            return
        }

        // --- 1. DETECCIÓN DE LLAMADAS (Prioridad Alta) ---
        val categoryCall = notification.category == Notification.CATEGORY_CALL
        val isDialer = packageName.contains("dialer") || packageName.contains("telecom")
        if (categoryCall || isDialer) {
            val caller = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() 
                ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                ?: "Desconocido"
            val isUnknown = ContactHelper.isContactUnknown(applicationContext, caller)
            val cat = if (isUnknown) "llamada_desconocida" else "llamada_entrante"
            processMessage(packageName, caller, "[LLAMADA] Actividad de voz detectada.", sbn.postTime, cat)
            return
        }

        // --- 2. EXTRACCIÓN PROFUNDA DE MENSAJES ---
        
        // Intentar extraer historial de mensajes (útil para WhatsApp/Telegram cuando se acumulan)
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            Log.d(TAG, "AUDITORÍA: Procesando ${messages.size} mensajes internos en EXTRA_MESSAGES")
            for (p in messages) {
                if (p is android.os.Bundle) {
                    val text = p.getCharSequence("text")?.toString() ?: ""
                    
                    // Intentar obtener el remitente de varias formas
                    val sender = p.getCharSequence("sender")?.toString() 
                        ?: p.getBundle("person")?.getCharSequence("name")?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                        ?: "Desconocido"
                        
                    val time = p.getLong("time", sbn.postTime)
                    
                    if (text.isNotBlank()) {
                        processMessage(packageName, sender, text, time, "mensaje")
                    }
                }
            }
            // NO HACER RETURN AQUÍ: A veces el mensaje más nuevo NO está en el historial 
            // pero sí en el texto principal de la notificación. El filtro de duplicados se encargará.
        }

        // Si no hay EXTRA_MESSAGES o para complementar, probar con el extractor estándar
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
        }

        // FALLBACK / REFUERZO: Lectura de campos de texto crudos
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() 
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: "Desconocido"
        
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        
        // Log para depuración profunda
        Log.v(TAG, "AUDITORÍA TEXTOS: title=$title, text=$text, bigText=$bigText, summary=$summaryText, sub=$subText")
        
        val possibleTexts = listOf(bigText, text, summaryText, infoText, subText)
            .filter { !it.isNullOrBlank() }
            .distinct()

        for (t in possibleTexts) {
            // Si el texto es algo como "3 nuevos mensajes", lo ignoramos para no ensuciar
            if (t!!.contains("nuevos mensajes") || t.contains("mensajes de")) continue
            processMessage(packageName, title, t, sbn.postTime, "mensaje")
        }
    }

    private fun processMessage(packageName: String, sender: String, text: String, time: Long, initialCategory: String) {
        if (text.isBlank() && initialCategory == "mensaje") return

        val fullText = "$sender: $text"
        val battery = DeviceStateHelper.getBatteryLevel(applicationContext)
        val connection = DeviceStateHelper.getConnectionType(applicationContext)

        // 1. Motor de Riesgo (Analizar el texto PRIMERO)
        // Si hay riesgo, ignoramos el filtro de duplicados: QUEREMOS recibirlo siempre.
        val match = RiskEngine.evaluate(fullText)
        if (match != null) {
            Log.e(TAG, "¡RIESGO DETECTADO! [$packageName] -> $fullText")
            mainHandler.post { Toast.makeText(applicationContext, "🚨 ALERTA DE RIESGO", Toast.LENGTH_SHORT).show() }
            upload(packageName, match.category, match.level.name, fullText, time, battery, connection)
            return
        }

        // 2. Filtro de duplicados solo para mensajes normales o informativos
        val msgHash = (packageName + sender + text.take(50) + initialCategory).hashCode()
        synchronized(processedHashes) {
            if (processedHashes.contains(msgHash)) {
                Log.v(TAG, "AUDITORÍA: Ignorando duplicado de $sender: $text")
                return
            }
            if (processedHashes.size > 100) {
                processedHashes.remove(processedHashes.iterator().next())
            }
            processedHashes.add(msgHash)
        }

        Log.d(TAG, "Evaluando para envío normal: $fullText")

        // 3. Detección de desconocidos
        if (initialCategory == "mensaje" || initialCategory.startsWith("llamada")) {
            val isUnknown = ContactHelper.isContactUnknown(applicationContext, sender)
            if (isUnknown && sender != "Desconocido" && !sender.startsWith("Grupo ")) {
                val cat = if (initialCategory.startsWith("llamada")) "llamada_desconocida" else "contacto_desconocido"
                upload(packageName, cat, RiskEngine.RiskLevel.MEDIUM.name, fullText, time, battery, connection)
            }
        }

        // 4. Si es un evento especial (foto, audio, etc), subirlo
        if (initialCategory != "mensaje") {
            upload(packageName, initialCategory, RiskEngine.RiskLevel.LOW.name, fullText, time, battery, connection)
        }
    }

    private fun upload(appName: String, category: String, level: String, text: String, time: Long, battery: Int, connection: String) {
        scope.launch {
            AlertUploader.sendAlert(applicationContext, appName, category, level, text, time, battery, connection)
        }
    }
}
