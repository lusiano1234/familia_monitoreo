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
    private const val BASE_URL = "https://TU-BACKEND.ejemplo.com/api/alerts"

    private val client = OkHttpClient()

    fun sendAlert(
        context: Context,
        sourceApp: String,
        category: String,
        level: String,
        fragment: String,
        timestamp: Long
    ) {
        Log.d(TAG, "Intentando enviar alerta: App=$sourceApp, Cat=$category, Fragment=$fragment")

        val deviceToken = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            .getString("device_token", "TOKEN_DE_PRUEBA") // Usar uno por defecto para ver logs

        val body = JSONObject().apply {
            put("sourceApp", sourceApp)
            put("category", category)
            put("level", level)
            put("fragment", fragment)
            put("timestamp", timestamp)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $deviceToken")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error en el servidor: ${response.code}")
                } else {
                    Log.d(TAG, "Alerta enviada con éxito")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error de red al enviar alerta: ${e.message}")
        }
    }
}
