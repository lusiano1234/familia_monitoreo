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
            Brand.VIVO -> "VIVO: En 'Batería -> Gestión de consumo de energía' elija 'No restringir'."
            else -> "Asegúrese de desactivar cualquier ahorro de energía para esta aplicación."
        }
    }

    /**
     * Intenta abrir el menú específico del fabricante o cae en Info de App
     */
    fun openManufacturerSettings(context: Context, brand: Brand) {
        val intent = Intent()
        try {
            when (brand) {
                Brand.XIAOMI -> {
                    intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                Brand.SAMSUNG -> {
                    intent.component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
                }
                Brand.VIVO -> {
                    intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
                Brand.MOTOROLA -> {
                    // Motorola suele usar una actividad de rendimiento
                    intent.action = "com.motorola.entities.intent.action.APP_MANAGEMENT"
                }
                else -> {
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", context.packageName, null)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback universal: Información de la Aplicación
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
