# Plan de Implementación: Captura Detallada de Todos los Mensajes (Sin Resúmenes)

Este plan asegura que el sistema capture cada mensaje individual enviado, incluso si llegan varios al mismo tiempo, mientras elimina las notificaciones de resumen ("2 mensajes nuevos") que ensucian el panel.

## Objetivos
1.  **Eliminar Resúmenes de Android**: Ignorar las notificaciones técnicas que agrupan chats (`FLAG_GROUP_SUMMARY`).
2.  **Captura Múltiple**: Si una notificación de WhatsApp contiene 3 mensajes nuevos, procesar los 3 individualmente en lugar de solo el último.
3.  **Deduplicación por Timestamp**: Utilizar la hora exacta de cada mensaje (según WhatsApp) para asegurar que se reporte cada interacción una sola vez, incluso si la notificación se actualiza repetidamente.
4.  **Preservar Blindaje**: Mantener el sistema de PIN, camuflaje y reporte de batería/red.

## Cambios Propuestos

### 1. App Android

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- **Filtro de Flags**: Al inicio de `onNotificationPosted`, descartar la notificación si es un resumen de grupo.
- **Procesamiento de Historial**:
    - Extraer la lista completa de mensajes de `MessagingStyle`.
    - Iterar por todos ellos.
    - Para cada mensaje, crear un identificador único combinando: `Remitente + Texto + HoraExactaDelMensaje`.
- **Memoria de Auditoría**: Ampliar el historial de mensajes procesados para manejar ráfagas de mensajes de forma eficiente.

## Plan de Verificación
1.  **Prueba de Ráfaga**: Enviar 3 mensajes distintos rápidamente (ej: "Hola", "Donde estas?", "ven a casa"). Verificar que el panel muestra los 3 como tarjetas separadas.
2.  **Prueba de Grupo**: Verificar que en un grupo con mucha actividad, cada mensaje de cada persona se reporte correctamente.
3.  **Limpieza de Resumen**: Provocar una notificación de "X mensajes nuevos" (recibiendo mensajes de varios chats) y confirmar que esa frase NO aparece en el panel.

---

**¿Deseas que proceda con esta captura detallada y masiva de mensajes?**
