package com.familia.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Escucha eventos críticos del sistema para despertar el monitoreo.
 */
class SystemReceiver : BroadcastReceiver() {

    private val TAG = "SystemReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Evento de sistema recibido: $action")

        // 1. Despertar los servicios de escucha
        NotificationCaptureService.forceRebind(context)

        // 2. Iniciar el servicio de primer plano (Aviso de monitoreo)
        val prefs = context.getSharedPreferences("consent", Context.MODE_PRIVATE)
        if (prefs.getBoolean("consent_given", false)) {
            val serviceIntent = Intent(context, ForegroundStatusService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
