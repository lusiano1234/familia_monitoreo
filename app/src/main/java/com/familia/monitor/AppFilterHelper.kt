package com.familia.monitor

import android.content.Context

object AppFilterHelper {
    private const val PREFS_NAME = "monitored_apps_prefs"

    data class AppDefinition(val packageName: String, val displayName: String)

    val availableApps = listOf(
        AppDefinition("com.whatsapp", "WhatsApp"),
        AppDefinition("com.whatsapp.w4b", "WhatsApp Business"),
        AppDefinition("com.instagram.android", "Instagram"),
        AppDefinition("org.telegram.messenger", "Telegram"),
        AppDefinition("com.facebook.orca", "Messenger"),
        AppDefinition("com.snapchat.android", "Snapchat"),
        AppDefinition("com.google.android.dialer", "Teléfono (Google)"),
        AppDefinition("com.samsung.android.dialer", "Teléfono (Samsung)"),
        AppDefinition("com.google.android.apps.messaging", "Mensajes SMS"),
        AppDefinition("com.zhiliaoapp.musically", "TikTok"),
        AppDefinition("com.discord", "Discord"),
        AppDefinition("com.twitter.android", "X (Twitter)"),
        AppDefinition("com.facebook.katana", "Facebook"),
        AppDefinition("com.google.android.youtube", "YouTube"),
        AppDefinition("tv.twitch.android.app", "Twitch")
    )

    fun isAppMonitored(context: Context, packageName: String): Boolean {
        // Por defecto, todas están activadas (true) si no se configuró nada aún
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(packageName, true)
    }

    fun setAppMonitored(context: Context, packageName: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(packageName, enabled).apply()
    }
}
