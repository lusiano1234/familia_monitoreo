package com.familia.monitor

import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

        // Mostrar el token del dispositivo
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        val token = devicePrefs.getString("device_token", "No generado")
        val tvToken = findViewById<TextView>(R.id.tv_token)
        tvToken.text = token

        findViewById<Button>(R.id.btn_copy).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Device Token", token)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Token copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_disable).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
