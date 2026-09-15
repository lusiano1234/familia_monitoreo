package com.familia.monitor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Accesible en cualquier momento desde el ícono de la app o la notificación
 * persistente. Muestra el estado real y permite desactivar el monitoreo.
 */
class StatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        val prefs = getSharedPreferences("consent", MODE_PRIVATE)
        val given = prefs.getBoolean("consent_given", false)

        findViewById<TextView>(R.id.tv_status).text = if (given) {
            "Monitoreo ACTIVO. Se están revisando notificaciones de apps de " +
            "mensajería en busca de señales de riesgo (extorsión, amenazas, " +
            "pedidos sospechosos)."
        } else {
            "Monitoreo INACTIVO."
        }

        findViewById<Button>(R.id.btn_disable).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
