package com.familia.monitor

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

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

        val checkbox = findViewById<CheckBox>(R.id.cb_consent)
        val btnContinue = findViewById<Button>(R.id.btn_continue)
        btnContinue.isEnabled = false

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            btnContinue.isEnabled = isChecked
        }

        btnContinue.setOnClickListener {
            // Generar y guardar un token único para el dispositivo
            val deviceToken = UUID.randomUUID().toString()
            getSharedPreferences("device", MODE_PRIVATE)
                .edit()
                .putString("device_token", deviceToken)
                .apply()

            getSharedPreferences("consent", MODE_PRIVATE)
                .edit()
                .putBoolean("consent_given", true)
                .putLong("consent_timestamp", System.currentTimeMillis())
                .apply()

            // Lleva al usuario al ajuste del sistema donde debe habilitar
            // manualmente el acceso a notificaciones (Android no permite
            // hacerlo automáticamente, por diseño).
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            startActivity(Intent(this, StatusActivity::class.java))
            finish()
        }
    }
}
