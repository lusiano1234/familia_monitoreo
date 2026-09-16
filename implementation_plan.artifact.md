# Plan de Implementación: Modo Verboso y Persistencia Total

Este plan tiene como objetivo forzar al sistema Android a mantener la aplicación viva y capturar absolutamente cualquier notificación para diagnosticar por qué WhatsApp no está siendo detectado.

## Objetivos
1.  **Visibilidad Total**: Mostrar un mensaje en pantalla (Toast) por **CUALQUIER** notificación que llegue al teléfono, sin importar la app.
2.  **Persistencia (Foreground Service)**: Activar la notificación permanente "Monitoreo Familiar activo" para evitar que Android mate la aplicación en segundo plano.
3.  **Soporte Ampliado**: Añadir soporte para WhatsApp Business y otras variantes.

## Cambios Propuestos

### 1. App Android

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- Eliminar el filtro inicial: Mostrar un Toast que diga `Recibido de: [paquete]` para **todas** las notificaciones.
- Añadir `com.whatsapp.w4b` a la lista de apps monitoreadas.
- Asegurar que el servicio esté vinculado correctamente.

#### [MODIFY] [ConsentActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/ConsentActivity.kt)
- Iniciar explícitamente el `ForegroundStatusService` al pulsar "Continuar".

#### [MODIFY] [StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)
- Añadir un chequeo para iniciar el servicio de estado si no está corriendo.

## Plan de Verificación
1.  **Prueba de Vida**: Recibir cualquier notificación (ej: Gmail, Sistema, YouTube). Debería aparecer un Toast negro diciendo el nombre de la app.
2.  **Prueba de WhatsApp**: Enviar un mensaje y observar si aparece el Toast de WhatsApp.
3.  **Prueba de Riesgo**: Enviar mensaje de riesgo y verificar subida al panel.

---

**¿Procedo con esta actualización de visibilidad total?**
