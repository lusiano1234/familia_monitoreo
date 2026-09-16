# Walkthrough - Captura Infalible y Seguridad de Sesión

Se han implementado mejoras críticas para garantizar que ningún mensaje se pierda, independientemente del modelo de teléfono, y para reforzar la privacidad del acceso físico.

## Mejoras Realizadas

### Captura Profunda (Deep Extraction)
*   **Análisis de Historial**: WhatsApp y Telegram suelen agrupar mensajes ("3 mensajes nuevos"). Ahora la app "abre" ese grupo y procesa cada mensaje individual contenido en el historial de la notificación (`EXTRA_MESSAGES`).
*   **Lectura Multicapa**: Si el texto principal está vacío, la app busca automáticamente en campos de respaldo como `BIG_TEXT` o `SUMMARY_TEXT`.
*   **Resiliencia de Red**: Se añadió un sistema de **3 reintentos automáticos** en `AlertUploader`. Si el teléfono pierde internet por un segundo al recibir el mensaje, la app esperará y volverá a intentar el envío del reporte.

### Seguridad de Sesión Automática
*   **Bloqueo al Salir**: Se implementó un sistema de protección que detecta cuando sales de la aplicación o bloqueas el teléfono. Al regresar, la app **siempre te pedirá la contraseña administrativa**.
*   **Privacidad Total**: Esto evita que alguien pueda ver el token o desactivar el monitoreo si el teléfono se queda desbloqueado en la pantalla de la app.

### Soporte Ampliado
*   Se añadieron firmas digitales para capturar notificaciones de servicios de llamadas del sistema y múltiples apps de SMS.

## Instrucciones para la Prueba en el segundo teléfono

1.  **Abre la app** y asegúrate de que el interruptor de "Monitoreo" esté encendido.
2.  **Sal al escritorio** y vuelve a entrar. Confirma que te pide la clave.
3.  **Envía un grupo de mensajes** (por ejemplo, 3 seguidos) al teléfono. Verifica que el panel los registre todos por separado.
4.  **Si algo falla**: Conecta el teléfono y revisa el Logcat con la etiqueta `NotificationCapture`. Verás líneas de `AUDITORÍA` que explican paso a paso qué está leyendo la app.

> [!IMPORTANT]
> Recuerda que en el segundo teléfono también debes activar manualmente el interruptor de **"Administrador de Dispositivo"** y el **"Acceso a Notificaciones"** para que el sistema funcione.
