# Plan de Implementación: Monitoreo Multimedia y Llamadas

Este plan expande las capacidades de detección para incluir interacciones no textuales, como llamadas entrantes, recepción de fotos, audios y videos, asegurando una vigilancia completa de las comunicaciones del dispositivo.

## Objetivos
1.  **Detección de Llamadas**: Identificar llamadas entrantes de WhatsApp y del sistema (Dialer), alertando especialmente si el contacto es desconocido.
2.  **Captura Multimedia**: Detectar cuando se recibe una foto, un audio (nota de voz) o un video.
3.  **Identificación Amigable**: Traducir los paquetes técnicos (`com.whatsapp`) a nombres legibles para los padres.
4.  **Categorización Detallada**: Diferenciar en el panel entre "Mensaje", "Llamada", "Foto" y "Audio".

## Cambios Propuestos

### 1. App Android

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- **Lógica de Llamadas**: Detectar notificaciones con categoría `Notification.CATEGORY_CALL`.
- **Análisis de Metadatos Multimedia**:
    - Extraer tipos MIME (`image/*`, `audio/*`, `video/*`) de los mensajes en `MessagingStyle`.
    - Buscar etiquetas de texto comunes (ej: "Foto", "Nota de voz", "Audio") como respaldo.
- **Normalización de Nombres de App**: Crear un mapa para mostrar "WhatsApp", "Instagram", "Telegram", "Teléfono" en lugar de los nombres de paquete.

#### [MODIFY] [RiskEngine.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/RiskEngine.kt)
- Añadir una nueva función `getMediaCategory` para clasificar el tipo de archivo recibido.

### 2. Frontend (Panel Web)

#### [MODIFY] [index.html](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/public/index.html)
- Actualizar los colores y etiquetas para las nuevas categorías multimedia.
- Ejemplo: Llamadas en un color distintivo (Azul) para diferenciarlas de los mensajes de riesgo (Rojo).

## Plan de Verificación
1.  **Prueba de Llamada**: Realizar una llamada de WhatsApp desde un número no registrado. El panel debe mostrar "LLAMADA DESCONOCIDA".
2.  **Prueba de Foto**: Enviar una foto por WhatsApp. El panel debe mostrar "FOTO RECIBIDA".
3.  **Prueba de Audio**: Enviar una nota de voz. El panel debe mostrar "AUDIO RECIBIDO".

---

**¿Deseas que proceda con la expansión multimedia para que no se escape ninguna interacción?**
