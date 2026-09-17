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
        
        // --- 1. MONITORIZACIÓN ---
        val swMonitoring = findViewById<MaterialSwitch>(R.id.sw_monitoring)
        swMonitoring.isChecked = !devicePrefs.getBoolean("monitoring_enabled", true)
        
        swMonitoring.setOnCheckedChangeListener { _, isChecked ->
            devicePrefs.edit().putBoolean("monitoring_enabled", !isChecked).apply()
            val msg = if (!isChecked) "Monitoreo REANUDADO" else "Monitoreo PAUSADO"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            updateUi()
        }

        // --- 2. GESTIÓN DE TOKEN ---
        val etToken = findViewById<TextInputEditText>(R.id.et_token_edit)
        val btnSaveToken = findViewById<Button>(R.id.btn_save_token)
        etToken.setText(devicePrefs.getString("device_token", ""))
        
        btnSaveToken.setOnClickListener {
            val newToken = etToken.text.toString().trim()
            if (newToken.isNotEmpty()) {
                devicePrefs.edit().putString("device_token", newToken).apply()
                Toast.makeText(this, "Token actualizado correctamente", Toast.LENGTH_SHORT).show()
                
                // Feedback visual: Ocultar teclado y quitar foco
                etToken.clearFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(etToken.windowToken, 0)
                
                updateUi()
            }
        }

        // --- 3. SUPERVISIÓN Y SEGURIDAD ---
        val swTotalSupervision = findViewById<MaterialSwitch>(R.id.sw_total_supervision)
        swTotalSupervision.isChecked = devicePrefs.getBoolean("total_supervision", false)
        swTotalSupervision.setOnCheckedChangeListener { _, isChecked ->
            devicePrefs.edit().putBoolean("total_supervision", isChecked).apply()
            val msg = if (isChecked) "Supervisión TOTAL activada" else "Supervisión total desactivada"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

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

        // --- 4. CONFIGURACIÓN DE APPS ---
        findViewById<Button>(R.id.btn_select_apps).setOnClickListener {
            val apps = AppFilterHelper.availableApps
            val appNames = apps.map { it.displayName }.toTypedArray()
            val checkedItems = apps.map { AppFilterHelper.isAppMonitored(this, it.packageName) }.toBooleanArray()

            AlertDialog.Builder(this)
                .setTitle("Apps a Monitorear")
                .setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
                    AppFilterHelper.setAppMonitored(this, apps[which].packageName, isChecked)
                }
                .setPositiveButton("Aceptar", null)
                .show()
        }

        // --- 5. REPARACIÓN DE PERMISOS ---
        findViewById<Button>(R.id.btn_fix_notif).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btn_fix_acc).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // --- 6. LOGS Y AYUDA ---
        findViewById<Button>(R.id.btn_view_logs).setOnClickListener {
            PinActivity.isNavigatingInternal = true
            startActivity(Intent(this, ChatLogActivity::class.java))
        }

        findViewById<Button>(R.id.btn_contacts).setOnClickListener {
            checkAndRequestContactsPermission()
        }

        findViewById<Button>(R.id.btn_help_permission).setOnClickListener {
            showRestrictedSettingsGuide()
        }

        findViewById<Button>(R.id.btn_app_info).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_battery).setOnClickListener {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }

        // --- 7. DIAGNÓSTICO ---
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

        findViewById<Button>(R.id.btn_refresh_service).setOnClickListener {
            Toast.makeText(this, "Reiniciando servicios...", Toast.LENGTH_SHORT).show()
            NotificationCaptureService.forceRebind(this)
            updateUi()
        }

        setupBrandOptimization()
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
        val btnBrandSettings = findViewById<Button>(R.id.btn_brand_settings)

        tvBrandTitle.text = "Optimización para su ${brand.name.lowercase().replaceFirstChar { it.uppercase() }}"
        
        btnBrandSettings.setOnClickListener {
            if (brand == ManufacturerHelper.Brand.VIVO) {
                AlertDialog.Builder(this)
                    .setTitle("Configuración Crítica VIVO")
                    .setMessage("En dispositivos VIVO, el sistema cierra la app agresivamente. \n\n" +
                            "POR FAVOR: \n" +
                            "1. Active 'Autoinicio'.\n" +
                            "2. Elija 'Alto consumo de energía en segundo plano'.\n" +
                            "3. Bloquee la app con un candado en la lista de apps abiertas.")
                    .setPositiveButton("IR A AJUSTES") { _, _ ->
                        ManufacturerHelper.openManufacturerSettings(this, brand)
                    }
                    .show()
            } else {
                ManufacturerHelper.openManufacturerSettings(this, brand)
            }
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

    private fun showRestrictedSettingsGuide() {
        AlertDialog.Builder(this)
            .setTitle("Desbloquear Ajustes (Android 13+)")
            .setMessage("Si el interruptor aparece en gris, sigue estos pasos:\n\n" +
                    "1. Presiona 'IR A INFO DE APP'.\n" +
                    "2. Toca los 3 puntos (⋮) arriba a la derecha.\n" +
                    "3. Elige 'Permitir ajustes restringidos'.\n" +
                    "4. Vuelve aquí e intenta de nuevo.")
            .setPositiveButton("IR A INFO DE APP") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("ENTENDIDO", null)
            .show()
    }

    private fun updateUi() {
        val devicePrefs = getSharedPreferences("device", MODE_PRIVATE)
        val isMonitoring = devicePrefs.getBoolean("monitoring_enabled", true)
        val isNotifEnabled = isNotificationServiceEnabled()
        val isAccEnabled = BackupCaptureService.isServiceRunning

        // 1. Estado Principal
        val tvStatusMain = findViewById<TextView>(R.id.tv_status_main)
        tvStatusMain.text = if (isMonitoring && isNotifEnabled) "● Monitoreo Protegido" else "⚠️ Configuración Pendiente"
        tvStatusMain.setTextColor(if (isMonitoring && isNotifEnabled) 0xFF10B981.toInt() else 0xFFF59E0B.toInt())

        // 2. Iconos de Checklist
        findViewById<TextView>(R.id.tv_status_notif_icon).text = if (isNotifEnabled) "✅" else "⚠️"
        findViewById<TextView>(R.id.tv_status_acc_icon).text = if (isAccEnabled) "✅" else "⚠️"

        // 3. Botones de Reparar (Ocultar si ya está bien)
        findViewById<Button>(R.id.btn_fix_notif).visibility = if (isNotifEnabled) View.GONE else View.VISIBLE
        findViewById<Button>(R.id.btn_fix_acc).visibility = if (isAccEnabled) View.GONE else View.VISIBLE

        // 4. Protección Anti-Borrado (Botón Dinámico)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)
        val isAdminActive = dpm.isAdminActive(componentName)
        val btnAdmin = findViewById<Button>(R.id.btn_admin)
        
        findViewById<TextView>(R.id.tv_status_admin_icon).text = if (isAdminActive) "✅" else "⚠️"
        
        if (isAdminActive) {
            btnAdmin.text = "DESACTIVAR PROTECCIÓN ANTI-BORRADO"
            btnAdmin.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF64748B.toInt()) // Gris pizarra
        } else {
            btnAdmin.text = "ACTIVAR PROTECCIÓN ANTI-BORRADO"
            btnAdmin.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFEF4444.toInt()) // Rojo
        }

        // 5. Estado del Token (Checklist)
        val hasToken = !devicePrefs.getString("device_token", "").isNullOrBlank()
        findViewById<TextView>(R.id.tv_status_token_icon).text = if (hasToken) "✅" else "⚠️"

        // 6. Última Actividad
        val tvLastActivity = findViewById<TextView>(R.id.tv_last_activity)
        val lastCapture = devicePrefs.getLong("last_capture_time", 0)
        if (lastCapture > 0) {
            val date = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastCapture))
            tvLastActivity.text = "Última actividad detectada: $date"
        }
    }

    override fun onStart() {
        super.onStart()
        if (!PinActivity.isSessionUnlocked) {
            startActivity(Intent(this, PinActivity::class.java))
            finish()
            return
        }
        
        // Watchdog automático al abrir la app
        if (isNotificationServiceEnabled() && !NotificationCaptureService.isServiceBound) {
            NotificationCaptureService.forceRebind(this)
        }
    }

    override fun onResume() {
        super.onResume()
        PinActivity.isNavigatingInternal = false
        updateUi()
    }

    override fun onStop() {
        super.onStop()
        if (!PinActivity.isNavigatingInternal) {
            PinActivity.isSessionUnlocked = false
        }
    }
}
