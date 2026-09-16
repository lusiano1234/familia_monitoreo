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

class StatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        updateUi()

        findViewById<Button>(R.id.btn_copy).setOnClickListener {
            val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
            val token = devicePrefs.getString("device_token", "")
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Token", token))
            Toast.makeText(this, "Copiado", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_request_contacts).setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 1)
        }

        findViewById<Button>(R.id.btn_disable).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun updateUi() {
        val prefs = getSharedPreferences("consent", MODE_PRIVATE)
        val given = prefs.getBoolean("consent_given", false)
        findViewById<TextView>(R.id.tv_status).text = if (given) "Monitoreo ACTIVO." else "Monitoreo INACTIVO."

        val token = getSharedPreferences("device", MODE_PRIVATE).getString("device_token", "S/N")
        findViewById<TextView>(R.id.tv_token).text = token

        val hasContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        findViewById<Button>(R.id.btn_request_contacts).apply {
            isEnabled = !hasContacts
            text = if (hasContacts) "Detección desconocidos: OK" else "Permitir detección de desconocidos"
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }
}
