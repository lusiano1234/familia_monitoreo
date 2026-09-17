package com.familia.monitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object ManufacturerHelper {

    enum class Brand {
        SAMSUNG, XIAOMI, MOTOROLA, VIVO, OTHER
    }

    fun getDeviceBrand(): Brand {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> Brand.SAMSUNG
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> Brand.XIAOMI
            manufacturer.contains("motorola") -> Brand.MOTOROLA
            manufacturer.contains("vivo") -> Brand.VIVO
            else -> Brand.OTHER
        }
    }

    fun getBrandGuide(brand: Brand): String {
        return when (brand) {
            Brand.SAMSUNG -> "SAMSUNG: Vaya a 'Límites de uso de fondo' y añada esta app a 'Aplicaciones nunca inactivas'."
            Brand.XIAOMI -> "XIAOMI: Active 'Inicio automático' y en 'Ahorro de batería' elija 'Sin restricciones'."
            Brand.MOTOROLA -> "MOTOROLA: En 'Sistema -> Rendimiento -> Gestión de aplicaciones' elija 'Permitir siempre'."
            Brand.VIVO -> "VIVO (CRÍTICO): \n1. Active 'Autoinicio'.\n2. En 'Consumo de energía' elija 'Alto consumo en segundo plano'.\n3. Bloquee la app en la lista de tareas recientes (deslizar hacia abajo)."
            else -> "Asegúrese de desactivar cualquier ahorro de energía para esta aplicación."
        }
    }

    /**
     * Intenta abrir el menú específico del fabricante o cae en Info de App
     */
    fun openManufacturerSettings(context: Context, brand: Brand) {
        val intents = mutableListOf<Intent>()

        when (brand) {
            Brand.XIAOMI -> {
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
            }
            Brand.SAMSUNG -> {
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")))
            }
            Brand.VIVO -> {
                intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.vivo.abe", "com.vivo.abe.process.ActivityPrmm")))
                intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")))
            }
            Brand.MOTOROLA -> {
                intents.add(Intent("com.motorola.entities.intent.action.APP_MANAGEMENT"))
            }
            else -> {}
        }

        // Siempre añadir Info de App como fallback final
        intents.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        })

        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return // Éxito
            } catch (e: Exception) {
                // Intentar el siguiente
            }
        }
    }
}
