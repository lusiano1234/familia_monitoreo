# Plan de Implementación: Extracción Profunda y Captura Multidispositivo

Este plan mejora la extracción de datos de las notificaciones para asegurar que ningún mensaje (individual, de grupo o acumulado) se pierda, especialmente en diferentes modelos de teléfonos.

## Objetivos
1.  **Extracción de Mensajes "Hijos"**: Asegurar que cuando Android agrupa notificaciones, capturemos el detalle de cada mensaje individual y no solo el resumen del grupo.
2.  **Análisis de Paquetes Ocultos**: Inspeccionar manualmente `EXTRA_MESSAGES` si el extractor automático de `MessagingStyle` falla.
3.  **Soporte para Mensajes Borrados/Editados**: Capturar el estado inicial de la notificación antes de que la app de mensajería la actualice o elimine.
4.  **Mayor Visibilidad de Error**: Loguear el contenido exacto de los "Extras" de la notificación en el Logcat para identificar por qué un teléfono específico no está reportando.

## Cambios Propuestos

### 1. App Android

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- **Mejora de `onNotificationPosted`**:
    - No ignorar las notificaciones marcadas como `FLAG_GROUP_SUMMARY` si contienen texto útil.
    - Implementar un iterador manual sobre `EXTRA_MESSAGES` (Bundle array) para capturar el historial completo enviado por WhatsApp.
    - Buscar en `EXTRA_TITLE` (Remitente) y `EXTRA_TEXT` (Contenido) de forma recursiva.
- **Normalización de Texto**: Asegurar que el filtro de duplicados no sea demasiado sensible a cambios de milisegundos en el timestamp.

#### [MODIFY] [AlertUploader.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/AlertUploader.kt)
- Añadir un pequeño reintento (Retry) en caso de que la red falle momentáneamente al enviar el reporte.

### 2. Panel Web (Visualización)
- No requiere cambios inmediatos, se centrará en la calidad de los datos recibidos.

## Plan de Verificación
1.  **Prueba de Mensajes Rápidos**: Enviar 5 mensajes de WhatsApp en menos de 2 segundos. Verificar que los 5 lleguen al panel.
2.  **Prueba de "Mensajes Nuevos"**: Dejar que se acumulen mensajes de diferentes personas y verificar que al llegar la notificación de "X mensajes de Y chats", la app desglose el contenido.
3.  **Prueba en Segundo Teléfono**: Confirmar que los mensajes que antes se perdían ahora se registran.

---

**¿Deseas que proceda con la extracción profunda de mensajes para que no se escape nada?**
