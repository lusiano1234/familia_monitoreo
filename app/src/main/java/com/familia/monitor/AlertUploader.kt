package com.familia.monitor

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object AlertUploader {

    private const val TAG = "AlertUploader"
    private const val BASE_URL = "https://familia-monitoreo.onrender.com/api/alerts"
    private val client = OkHttpClient()

    fun sendAlert(
        context: Context,
        sourceApp: String,
        category: String,
        level: String,
        fragment: String,
        timestamp: Long,
        battery: Int = -1,
        connection: String = "Desconocido"
    ) {
        val deviceToken = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            .getString("device_token", null)

        if (deviceToken == null) {
            Log.e(TAG, "ERROR: No hay token de dispositivo configurado.")
            return
        }

        val bodyJson = JSONObject().apply {
            put("sourceApp", sourceApp)
            put("category", category)
            put("level", level)
            put("fragment", fragment)
            put("timestamp", timestamp)
            put("batteryLevel", battery)
            put("connectionType", connection)
        }
        
        val body = bodyJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $deviceToken")
            .post(body)
            .build()

        var success = false
        var attempts = 0
        val maxAttempts = 2

        while (!success && attempts < maxAttempts) {
            attempts++
            var shouldBreak = false
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "ÉXITO: Reporte enviado correctamente.")
                        success = true
                    } else {
                        Log.e(TAG, "ERROR SERVIDOR: Código ${response.code}")
                        if (response.code == 401) shouldBreak = true
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "ERROR RED: ${e.message} (Intento $attempts)")
                if (attempts < maxAttempts) Thread.sleep(1000)
            }
            if (shouldBreak) break
        }

        if (!success) {
            Log.d(TAG, "Guardando en buffer offline...")
            OfflineBufferHelper.saveAlert(context, bodyJson)
        }
    }
}
