package com.familia.monitor

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
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
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protege la app contra desinstalación.")
                }
                startActivity(intent)
            } else {
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

        // 5. Botones de Permisos y Ayuda
        findViewById<Button>(R.id.btn_disable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btn_app_info).setOnClickListener {
            openAppInfo()
        }

        findViewById<Button>(R.id.btn_battery).setOnClickListener {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_help_permission).setOnClickListener {
            showRestrictedSettingsGuide()
        }

        updateUi()
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun updateUi() {
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        val isMonitoring = devicePrefs.getBoolean("monitoring_enabled", true)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)
        val isAdminActive = dpm.isAdminActive(componentName)
        val isServiceRunning = isNotificationServiceEnabled()

        // Banner de advertencia si no hay permiso
        val cardWarning = findViewById<MaterialCardView>(R.id.card_permission_warning)
        cardWarning.visibility = if (isServiceRunning) View.GONE else View.VISIBLE

        val tvStatus = findViewById<TextView>(R.id.tv_status)
        tvStatus.text = when {
            !isServiceRunning -> "❌ Permiso de lectura bloqueado por Android"
            isMonitoring -> "● El sistema está capturando actividad"
            else -> "○ El sistema está en pausa"
        }
        tvStatus.setTextColor(if (isMonitoring && isServiceRunning) 0xFF10B981.toInt() else 0xFFEF4444.toInt())

        val btnAdmin = findViewById<Button>(R.id.btn_admin)
        btnAdmin.text = if (isAdminActive) "DESACTIVAR PROTECCIÓN ANTI-BORRADO" else "ACTIVAR PROTECCIÓN ANTI-BORRADO"
        btnAdmin.setBackgroundColor(if (isAdminActive) 0xFF6B7280.toInt() else 0xFFEF4444.toInt())
    }

    private fun openAppInfo() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun showRestrictedSettingsGuide() {
        AlertDialog.Builder(this)
            .setTitle("Desbloquear Ajustes (Android 13+)")
            .setMessage("Si el interruptor aparece en gris, sigue estos pasos:\n\n" +
                    "1. Presiona 'IR A INFO DE APP'.\n" +
                    "2. Toca los 3 puntos (⋮) arriba a la derecha.\n" +
                    "3. Elige 'Permitir ajustes restringidos'.\n" +
                    "4. Vuelve aquí e intenta de nuevo.")
            .setPositiveButton("IR A INFO DE APP") { _, _ -> openAppInfo() }
            .setNegativeButton("ENTENDIDO", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        if (!PinActivity.isSessionUnlocked) {
            startActivity(Intent(this, PinActivity::class.java))
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        PinActivity.isSessionUnlocked = false
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }
}
