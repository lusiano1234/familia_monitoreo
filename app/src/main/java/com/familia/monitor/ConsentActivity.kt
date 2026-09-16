package com.familia.monitor

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged

/**
 * Esta pantalla es OBLIGATORIA y bloqueante: nada se activa hasta que
 * la persona dueña del dispositivo marque el checkbox de consentimiento.
 * No hay forma de omitirla ni de ocultarla.
 */
class ConsentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent)

        val explanation = findViewById<TextView>(R.id.tv_explanation)
        explanation.text = """
            Esta app fue instalada por tu familia para ayudar a detectar señales
            de riesgo (extorsión, acoso, pedidos sospechosos) en tus apps de
            mensajería.

            Qué hace:
            • Lee el TEXTO de las notificaciones de apps de mensajería.
            • Busca patrones de riesgo (amenazas, pedidos de dinero o fotos,
              frases como "no le digas a nadie").
            • Solo envía a tus padres una alerta cuando detecta algo de riesgo,
              junto con el fragmento de texto que la disparó.

            Qué NO hace:
            • No envía todos tus mensajes ni graba conversaciones completas.
            • No accede a tus cuentas ni contraseñas.
            • No se puede desinstalar sin que quede aviso (ver ícono y
              notificación permanente mientras esté activa).

            Podés desactivarla en cualquier momento desde Ajustes > Apps >
            Monitoreo Familiar.
        """.trimIndent()

        val etToken = findViewById<EditText>(R.id.et_token)
        val etAdminPassword = findViewById<EditText>(R.id.et_admin_password)
        val checkbox = findViewById<CheckBox>(R.id.cb_consent)
        val btnContinue = findViewById<Button>(R.id.btn_continue)
        btnContinue.isEnabled = false

        fun updateButtonState() {
            val hasToken = etToken.text.toString().trim().isNotEmpty()
            val hasPassword = etAdminPassword.text.toString().trim().isNotEmpty()
            val hasConsent = checkbox.isChecked
            btnContinue.isEnabled = hasToken && hasPassword && hasConsent
        }

        etToken.doAfterTextChanged { updateButtonState() }
        etAdminPassword.doAfterTextChanged { updateButtonState() }
        checkbox.setOnCheckedChangeListener { _, _ -> updateButtonState() }

        btnContinue.setOnClickListener {
            val typedToken = etToken.text.toString().trim()
            val typedPassword = etAdminPassword.text.toString().trim()
            
            // Guardar configuración
            getSharedPreferences("device", MODE_PRIVATE)
                .edit()
                .putString("device_token", typedToken)
                .putString("admin_password", typedPassword)
                .putBoolean("monitoring_enabled", true)
                .apply()

            getSharedPreferences("consent", MODE_PRIVATE)
                .edit()
                .putBoolean("consent_given", true)
                .putLong("consent_timestamp", System.currentTimeMillis())
                .apply()

            // Desbloquear sesión al registrar inicialmente
            PinActivity.isSessionUnlocked = true

            // Iniciar servicio en primer plano
            val serviceIntent = Intent(this, ForegroundStatusService::class.java)
            startForegroundService(serviceIntent)

            // Aviso especial para Android 13+ antes de ir a ajustes
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                AlertDialog.Builder(this)
                    .setTitle("Paso Importante (Android 13+)")
                    .setMessage("Si al intentar activar el permiso ves que el interruptor está en gris, deberás tocar los 3 puntos (⋮) en 'Información de la aplicación' y elegir 'Permitir ajustes restringidos'.")
                    .setPositiveButton("ENTENDIDO") { _, _ ->
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        startActivity(Intent(this, StatusActivity::class.java))
                        finish()
                    }
                    .show()
            } else {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                startActivity(Intent(this, StatusActivity::class.java))
                finish()
            }
        }
    }
}
