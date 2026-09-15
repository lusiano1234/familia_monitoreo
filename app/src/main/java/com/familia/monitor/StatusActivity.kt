package com.familia.monitor

import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Accesible en cualquier momento desde el ícono de la app o la notificación
 * persistente. Muestra el estado real y permite desactivar el monitoreo.
 */
class StatusActivity : AppCompatActivity() {

    private val CONTACTS_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        updateUi()

        findViewById<Button>(R.id.btn_copy).setOnClickListener {
            val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
            val token = devicePrefs.getString("device_token", "No generado")
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Device Token", token)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Token copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_request_contacts).setOnClickListener {
            requestContactsPermission()
        }

        findViewById<Button>(R.id.btn_disable).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun updateUi() {
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
        findViewById<TextView>(R.id.tv_token).text = token

        // Estado del permiso de contactos
        val hasContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val tvContacts = findViewById<TextView>(R.id.tv_contacts_status)
        val btnContacts = findViewById<Button>(R.id.btn_request_contacts)

        if (hasContacts) {
            tvContacts.text = "Acceso a contactos: CONCEDIDO"
            tvContacts.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            btnContacts.isEnabled = false
            btnContacts.text = "Detección de desconocidos activa"
        } else {
            tvContacts.text = "Acceso a contactos: NO CONCEDIDO"
            tvContacts.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            btnContacts.isEnabled = true
        }
    }

    private fun requestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), CONTACTS_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de contactos concedido", Toast.LENGTH_SHORT).show()
                updateUi()
            } else {
                Toast.makeText(this, "Permiso denegado. No se podrán detectar desconocidos.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }
}
