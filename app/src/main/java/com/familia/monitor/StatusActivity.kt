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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Accesible en cualquier momento desde el ícono de la app o la notificación
 * persistente. Muestra el estado real y permite desactivar el monitoreo.
 */
class StatusActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        val prefs = getSharedPreferences("consent", MODE_PRIVATE)
        val given = prefs.getBoolean("consent_given", false)

        if (given) {
            val serviceIntent = Intent(this, ForegroundStatusService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        findViewById<TextView>(R.id.tv_status).text = if (given) {
            "Monitoreo ACTIVO."
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
            Toast.makeText(this, "Token copiado", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_test).setOnClickListener {
            Toast.makeText(this, "Enviando prueba...", Toast.LENGTH_SHORT).show()
            val battery = DeviceStateHelper.getBatteryLevel(applicationContext)
            val connection = DeviceStateHelper.getConnectionType(applicationContext)
            
            scope.launch {
                AlertUploader.sendAlert(
                    context = applicationContext,
                    sourceApp = "com.familia.monitor.test",
                    category = "prueba_manual",
                    level = "LOW",
                    fragment = "Esta es una alerta de prueba manual desde el dispositivo.",
                    timestamp = System.currentTimeMillis(),
                    battery = battery,
                    connection = connection
                )
            }
        }

        findViewById<Button>(R.id.btn_disable).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
