# Tareas: Gestión de Seguridad y Configuración Avanzada

- [x] Modificar `activity_consent.xml` para incluir campo de "Contraseña Administrativa"
- [x] Actualizar `ConsentActivity.kt` para guardar la contraseña administrativa
- [x] Actualizar `PinActivity.kt` para validar contra la contraseña guardada
- [x] Rediseñar `activity_status.xml`
    - [x] Añadir interruptor (Switch) para activar/desactivar monitoreo
    - [x] Hacer el campo de Token editable
    - [x] Añadir botón para desactivar protección anti-borrado
- [x] Actualizar `StatusActivity.kt`
    - [x] Lógica para activar/desactivar monitoreo
    - [x] Lógica para guardar cambios de Token
    - [x] Lógica para remover Administrador de Dispositivo
- [x] Modificar `NotificationCaptureService.kt` para respetar el interruptor de monitoreo
- [x] Verificación
    - [x] Probar bloqueo/desbloqueo de monitoreo
    - [x] Probar cambio de token
    - [x] Probar desactivación de protección anti-borrado
