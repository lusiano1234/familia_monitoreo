package com.familia.monitor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla de seguridad para evitar que el niño cambie los ajustes.
 * Valida contra la contraseña administrativa configurada en el inicio.
 */
class PinActivity : AppCompatActivity() {

    private val TAG = "PinActivity"

    companion object {
        var isSessionUnlocked: Boolean = false
        var isNavigatingInternal: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("consent", MODE_PRIVATE)
        val given = prefs.getBoolean("consent_given", false)

        if (!given) {
            startActivity(Intent(this, ConsentActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_pin)

        val etPin = findViewById<EditText>(R.id.et_pin)
        val btnUnlock = findViewById<Button>(R.id.btn_unlock)

        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        val adminPassword = devicePrefs.getString("admin_password", "1234") ?: "1234"

        // LOG DE SEGURIDAD (Solo para el desarrollador en Android Studio)
        Log.d(TAG, "DEBUG: La contraseña esperada es: $adminPassword")

        btnUnlock.setOnClickListener {
            val input = etPin.text.toString().trim()
            if (input == adminPassword) {
                isSessionUnlocked = true
                startActivity(Intent(this, StatusActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Contraseña Incorrecta", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Intento fallido. Ingresado: '$input', Esperado: '$adminPassword'")
                etPin.text.clear()
            }
        }
    }
}
