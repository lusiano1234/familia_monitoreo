# Plan de Implementación: Gestión de Seguridad y Configuración Avanzada

Este plan permite a los padres controlar funciones críticas de la aplicación (monitoreo, token y protección anti-borrado) utilizando la misma contraseña del panel administrativo para garantizar que el niño no pueda desactivar la protección.

## Objetivos
1.  **Validación de Contraseña Administrativa**: Sincronizar el acceso a la app con la clave del panel web.
2.  **Control de Monitoreo**: Añadir un interruptor maestro para pausar/activar la captura de mensajes.
3.  **Gestión de Token**: Permitir la edición del token de vinculación directamente desde la app.
4.  **Gestión de Protección Anti-Borrado**: Permitir desactivar el permiso de Administrador de Dispositivo de forma sencilla (pero protegida por clave).

## Cambios Propuestos

### 1. App Android

#### [MODIFY] [PinActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/PinActivity.kt)
- Reemplazar el PIN fijo `1234` por una validación de contraseña.
- *Nota*: La contraseña se validará contra el servidor o se guardará de forma segura durante la configuración inicial para permitir acceso offline.

#### [MODIFY] [res/layout/activity_status.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_status.xml)
- **Interruptor Maestro**: Añadir un `MaterialSwitch` para activar/desactivar el monitoreo.
- **Campo de Token**: Cambiar el texto estático por un `TextInputLayout` con `TextInputEditText`.
- **Botón de Protección**: Cambiar el botón de "Activar" por uno dinámico que también permita "Desactivar" si ya está activo.

#### [MODIFY] [StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)
- **Lógica de Desactivación**: Implementar `dpm.removeActiveAdmin(componentName)` para quitar la protección anti-borrado.
- **Persistencia**: Guardar el estado `is_monitoring_enabled` en SharedPreferences.

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- Añadir un chequeo al inicio de `onNotificationPosted`: si el monitoreo está apagado, ignorar el mensaje.

## Plan de Verificación
1.  **Seguridad**: Intentar desactivar el monitoreo sin poner la clave correcta.
2.  **Desactivación de Admin**: Activar la protección, confirmar que no se puede borrar la app, luego desactivarla desde el botón protegido y confirmar que ahora sí se puede desinstalar.
3.  **Edición de Token**: Cambiar el token y verificar que las alertas lleguen al nuevo destino.

---

**¿Deseas que proceda con esta actualización para darte control total sobre la seguridad de la app?**
