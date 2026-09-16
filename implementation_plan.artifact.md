# Plan de Implementación: Expansión de Cobertura de Aplicaciones

Este plan tiene como objetivo ampliar el monitoreo a aplicaciones adicionales como TikTok, Discord y otras redes sociales populares para garantizar que no haya puntos ciegos en la supervisión de seguridad.

## Objetivos
1.  **Monitoreo de TikTok**: Añadir los paquetes de TikTok e implementar la captura de sus notificaciones de mensajes directos.
2.  **Ampliación Social**: Incluir soporte para Discord, X (Twitter) y otras apps de interacción social.
3.  **Mantenimiento de Nombres Amigables**: Asegurar que en el panel estas nuevas apps aparezcan con sus nombres reales y no con códigos técnicos.

## Cambios Propuestos

### 1. App Android

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- Añadir a `MONITORED_PACKAGES`:
    - `com.zhiliaoapp.musically` (TikTok Internacional)
    - `com.ss.android.ugc.trill` (TikTok variante)
    - `com.discord` (Discord)
    - `com.twitter.android` (X / Twitter)
    - `com.google.android.youtube` (YouTube - Comentarios/Mensajes)
- Actualizar el mapa `APP_NAMES` con las etiquetas correspondientes.

## Plan de Verificación
1.  **Prueba de TikTok**: Enviar un mensaje directo a la cuenta de TikTok en el teléfono del niño. Verificar que el panel web registra el mensaje y el nombre del remitente.
2.  **Prueba de Discord/X**: Confirmar que las notificaciones de estas apps disparan el proceso de evaluación de riesgo.
3.  **Robustez de Nombres**: Asegurar que en el panel web se lea claramente "TikTok" o "Discord".

---

**¿Deseas que proceda con la expansión a TikTok y estas redes sociales adicionales?**
