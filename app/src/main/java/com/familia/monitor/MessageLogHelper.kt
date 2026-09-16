package com.familia.monitor

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object MessageLogHelper {
    private const val PREFS_NAME = "chat_logs"
    private const val KEY_MESSAGES = "captured_messages"
    private const val MAX_MESSAGES = 500

    fun saveMessage(context: Context, appName: String, sender: String, text: String, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_MESSAGES, "[]")
        
        try {
            val array = JSONArray(currentJson)
            val newMessage = JSONObject().apply {
                put("appName", appName)
                put("sender", sender)
                put("text", text)
                put("timestamp", timestamp)
            }
            
            // Insert at the beginning (newest first)
            val newArray = JSONArray()
            newArray.put(newMessage)
            for (i in 0 until Math.min(array.length(), MAX_MESSAGES - 1)) {
                newArray.put(array.get(i))
            }
            
            prefs.edit { 
                putString(KEY_MESSAGES, newArray.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMessages(context: Context): List<CapturedMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_MESSAGES, "[]")
        val result = mutableListOf<CapturedMessage>()
        
        try {
            val array = JSONArray(currentJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(CapturedMessage(
                    obj.getString("appName"),
                    obj.getString("sender"),
                    obj.getString("text"),
                    obj.getLong("timestamp")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun clearMessages(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_MESSAGES)
        }
    }

    data class CapturedMessage(
        val appName: String,
        val sender: String,
        val text: String,
        val timestamp: Long
    )
}
