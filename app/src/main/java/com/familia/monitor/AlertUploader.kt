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

    // Reemplazar por la URL real del backend (HTTPS obligatorio en producción).
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

        Log.d(TAG, "Enviando reporte ($category) a $BASE_URL")

        if (deviceToken == null) {
            Log.e(TAG, "ERROR: No hay token de dispositivo configurado. Abortando.")
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
        }.toString()
        
        val body = bodyJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $deviceToken")
            .post(body)
            .build()

        // Lógica de reintento simple para asegurar el envío
        var success = false
        var attempts = 0
        val maxAttempts = 3

        while (!success && attempts < maxAttempts) {
            attempts++
            var shouldBreak = false
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "ÉXITO: Reporte enviado correctamente (Intento $attempts)")
                        success = true
                    } else {
                        Log.e(TAG, "ERROR SERVIDOR: Código ${response.code} (Intento $attempts)")
                        if (response.code == 401) shouldBreak = true
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "ERROR RED: ${e.message} (Reintentando en 2s...)")
                if (attempts < maxAttempts) Thread.sleep(2000)
            }
            if (shouldBreak) break
        }
    }
}
