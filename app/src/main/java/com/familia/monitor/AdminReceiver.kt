package com.familia.monitor

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Este receptor permite que la app sea Administrador de Dispositivo.
 * Una vez activado, Android impedirá la desinstalación directa de la app
 * hasta que este permiso sea revocado manualmente.
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Protección contra borrado: ACTIVADA", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Protección contra borrado: DESACTIVADA", Toast.LENGTH_SHORT).show()
    }
}
