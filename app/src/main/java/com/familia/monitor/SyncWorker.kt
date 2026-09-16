package com.familia.monitor

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "SyncWorker"
    private val client = OkHttpClient()
    private val BASE_URL = "https://familia-monitoreo.onrender.com/api/alerts"

    override suspend fun doWork(): Result {
        val pendingAlerts = OfflineBufferHelper.getPendingAlerts(applicationContext)

        if (pendingAlerts.isEmpty()) return Result.success()

        val deviceToken = applicationContext.getSharedPreferences("device", Context.MODE_PRIVATE)
            .getString("device_token", null) ?: return Result.failure()

        Log.d(TAG, "Sincronizando ${pendingAlerts.size} alertas offline...")

        var allSuccess = true
        for (bodyJson in pendingAlerts) {
            // Marcamos como sincronizado
            bodyJson.put("connectionType", "${bodyJson.optString("connectionType")} (Sincronizado)")
            
            val body = bodyJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $deviceToken")
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        OfflineBufferHelper.removeAlert(applicationContext, bodyJson)
                        Log.d(TAG, "Alerta offline enviada con éxito.")
                    } else {
                        allSuccess = false
                    }
                }
            } catch (e: Exception) {
                allSuccess = false
                Log.e(TAG, "Error en sincronización: ${e.message}")
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }
}
