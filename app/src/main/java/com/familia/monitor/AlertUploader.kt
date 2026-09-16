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
        timestamp: Long
    ) {
        val deviceToken = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            .getString("device_token", null)

        Log.d(TAG, "Iniciando envío de alerta. Token actual: $deviceToken")
        Log.d(TAG, "URL de destino: $BASE_URL")

        if (deviceToken == null) {
            Log.e(TAG, "ERROR: No hay token de dispositivo configurado. Abortando envío.")
            return
        }

        val bodyJson = JSONObject().apply {
            put("sourceApp", sourceApp)
            put("category", category)
            put("level", level)
            put("fragment", fragment)
            put("timestamp", timestamp)
        }.toString()
        
        Log.d(TAG, "Cuerpo del JSON: $bodyJson")

        val body = bodyJson.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $deviceToken")
            .post(body)
            .build()

        try {
            Log.d(TAG, "Ejecutando petición HTTP POST...")
            val response = client.newCall(request).execute()
            
            val responseCode = response.code
            val responseBody = response.body?.string() ?: "Cuerpo vacío"
            
            if (!response.isSuccessful) {
                Log.e(TAG, "FALLO EN EL SERVIDOR. Código: $responseCode")
                Log.e(TAG, "Respuesta del servidor: $responseBody")
                if (responseCode == 401) {
                    Log.e(TAG, "Sugerencia: El token del dispositivo puede ser inválido o haber expirado.")
                }
            } else {
                Log.d(TAG, "ÉXITO TOTAL. Alerta recibida por el servidor. Código: $responseCode")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "ERROR DE CONEXIÓN CRÍTICO: ${e.message}")
            Log.e(TAG, "Verifica si el servidor está caído o si el teléfono no tiene internet.")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR INTERNO EN UPLOADER: ${e.message}")
        }
    }
}
