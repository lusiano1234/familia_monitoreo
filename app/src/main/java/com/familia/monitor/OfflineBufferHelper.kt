package com.familia.monitor

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object OfflineBufferHelper {
    private const val PREFS_NAME = "offline_buffer"
    private const val KEY_ALERTS = "pending_alerts"
    private const val TAG = "OfflineBuffer"

    fun saveAlert(context: Context, body: JSONObject) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentAlertsStr = prefs.getString(KEY_ALERTS, "[]")
        try {
            val array = JSONArray(currentAlertsStr)
            array.put(body)
            prefs.edit().putString(KEY_ALERTS, array.toString()).apply()
            Log.d(TAG, "Alerta guardada en buffer offline. Total: ${array.length()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando en buffer: ${e.message}")
        }
    }

    fun getPendingAlerts(context: Context): List<JSONObject> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentAlertsStr = prefs.getString(KEY_ALERTS, "[]")
        val list = mutableListOf<JSONObject>()
        try {
            val array = JSONArray(currentAlertsStr)
            for (i in 0 until array.length()) {
                list.add(array.getJSONObject(i))
            }
        } catch (e: Exception) {}
        return list
    }

    fun removeAlert(context: Context, alert: JSONObject) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentAlertsStr = prefs.getString(KEY_ALERTS, "[]")
        try {
            val array = JSONArray(currentAlertsStr)
            val newArray = JSONArray()
            val alertStr = alert.toString()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                if (item.toString() != alertStr) {
                    newArray.put(item)
                }
            }
            prefs.edit().putString(KEY_ALERTS, newArray.toString()).apply()
        } catch (e: Exception) {}
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
