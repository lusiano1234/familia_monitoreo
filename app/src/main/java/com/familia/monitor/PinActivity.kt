package com.familia.monitor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla de seguridad para evitar que el niño cambie los ajustes.
 * Por defecto el PIN es 1234 (puedes cambiarlo aquí o hacerlo dinámico luego).
 */
class PinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("consent", MODE_PRIVATE)
        val given = prefs.getBoolean("consent_given", false)

        // Si no se ha configurado la app aún, vamos directo al consentimiento
        if (!given) {
            startActivity(Intent(this, ConsentActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_pin)

        val etPin = findViewById<EditText>(R.id.et_pin)
        val btnUnlock = findViewById<Button>(R.id.btn_unlock)

        // Cambiar el PIN genérico por la contraseña guardada en el dispositivo
        val adminPassword = getSharedPreferences("device", MODE_PRIVATE)
            .getString("admin_password", "1234") // Fallback a 1234 si falla algo

        btnUnlock.setOnClickListener {
            val input = etPin.text.toString().trim()
            if (input == adminPassword) {
                startActivity(Intent(this, StatusActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Contraseña Incorrecta", Toast.LENGTH_SHORT).show()
                etPin.text.clear()
            }
        }
    }
}
