package com.familia.monitor

import android.app.admin.DevicePolicyManager
import android.content.ClipboardManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatusActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        
        // 1. Interruptor Maestro de Monitoreo
        val swMonitoring = findViewById<MaterialSwitch>(R.id.sw_monitoring)
        swMonitoring.isChecked = devicePrefs.getBoolean("monitoring_enabled", true)
        
        swMonitoring.setOnCheckedChangeListener { _, isChecked ->
            devicePrefs.edit().putBoolean("monitoring_enabled", isChecked).apply()
            val msg = if (isChecked) "Monitoreo REANUDADO" else "Monitoreo PAUSADO"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            updateUi()
        }

        // 2. Gestión de Token
        val etToken = findViewById<TextInputEditText>(R.id.et_token_edit)
        val btnSaveToken = findViewById<Button>(R.id.btn_save_token)
        
        etToken.setText(devicePrefs.getString("device_token", ""))
        
        btnSaveToken.setOnClickListener {
            val newToken = etToken.text.toString().trim()
            if (newToken.isNotEmpty()) {
                devicePrefs.edit().putString("device_token", newToken).apply()
                Toast.makeText(this, "Token actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Protección Anti-Borrado (Administrador)
        val btnAdmin = findViewById<Button>(R.id.btn_admin)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)

        btnAdmin.setOnClickListener {
            if (!dpm.isAdminActive(componentName)) {
                // Activar
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protege la app contra desinstalación.")
                }
                startActivity(intent)
            } else {
                // Desactivar (Protegido por el PIN que ya pusimos para entrar aquí)
                dpm.removeActiveAdmin(componentName)
                Toast.makeText(this, "Protección anti-borrado DESACTIVADA", Toast.LENGTH_LONG).show()
                updateUi()
            }
        }

        // 4. Botón de Prueba
        findViewById<Button>(R.id.btn_test).setOnClickListener {
            Toast.makeText(this, "Enviando señal...", Toast.LENGTH_SHORT).show()
            val battery = DeviceStateHelper.getBatteryLevel(applicationContext)
            val connection = DeviceStateHelper.getConnectionType(applicationContext)
            
            scope.launch {
                AlertUploader.sendAlert(
                    context = applicationContext,
                    sourceApp = "Sincronizador",
                    category = "prueba_manual",
                    level = "LOW",
                    fragment = "Señal de prueba enviada desde el dispositivo.",
                    timestamp = System.currentTimeMillis(),
                    battery = battery,
                    connection = connection
                )
            }
        }

        // 5. Botón Avanzado (Ajustes de sistema)
        findViewById<Button>(R.id.btn_disable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        updateUi()
    }

    private fun updateUi() {
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        val isMonitoring = devicePrefs.getBoolean("monitoring_enabled", true)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)
        val isAdminActive = dpm.isAdminActive(componentName)

        val tvStatus = findViewById<TextView>(R.id.tv_status)
        tvStatus.text = if (isMonitoring) "● El sistema está capturando actividad" else "○ El sistema está en pausa"
        tvStatus.setTextColor(if (isMonitoring) 0xFF10B981.toInt() else 0xFFEF4444.toInt())

        val btnAdmin = findViewById<Button>(R.id.btn_admin)
        btnAdmin.text = if (isAdminActive) "DESACTIVAR PROTECCIÓN ANTI-BORRADO" else "ACTIVAR PROTECCIÓN ANTI-BORRADO"
        btnAdmin.setBackgroundColor(if (isAdminActive) 0xFF6B7280.toInt() else 0xFFEF4444.toInt())
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }
}
