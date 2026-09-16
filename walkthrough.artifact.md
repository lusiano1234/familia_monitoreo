# Walkthrough - Captura Detallada y Seguridad Persistente

Se han aplicado mejoras de ingeniería para garantizar que cada mensaje sea capturado individualmente y para blindar el acceso físico a la aplicación.

## Mejoras Realizadas

### Captura Detallada (Anti-Resúmenes)
*   **Filtro de Resúmenes**: La aplicación ahora identifica y descarta las notificaciones genéricas de Android como "2 mensajes nuevos". Solo se procesan las notificaciones que contienen el contenido real del mensaje.
*   **Desglose de Historial**: Si una notificación trae varios mensajes acumulados (común en WhatsApp), el sistema los "abre" y procesa cada uno de forma independiente.
*   **Deduplicación por Segundos**: Se implementó un sistema que compara el contenido y la hora exacta del mensaje. Esto permite recibir mensajes idénticos si se enviaron en momentos distintos, pero evita duplicados por actualizaciones de la propia aplicación de chat.

### Seguridad de Sesión Inmediata
*   **Bloqueo al Salir**: Se ha reforzado el sistema de seguridad. En cuanto sales de la pantalla de la app (ir al inicio, bloquear el teléfono o cambiar de app), la sesión se cierra automáticamente.
*   **Re-validación Obligatoria**: Al regresar al "Servicio de Sincronización", siempre se te solicitará la contraseña administrativa para ver el estado o el token.

### Soporte Multidispositivo
*   Se refinó la búsqueda de texto en campos ocultos del sistema para asegurar que teléfonos de distintas marcas (Samsung, Xiaomi, Motorola, etc.) reporten con la misma fidelidad.

## Instrucciones para la Verificación

1.  **Seguridad**: Abre la app, ingresa tu clave, sal al escritorio y vuelve a entrar. Confirma que se ha bloqueado de nuevo.
2.  **Prueba de Mensajes**: Envía 2 o 3 mensajes seguidos de WhatsApp al teléfono monitoreado.
    *   Verifica que en el panel web aparezcan los mensajes **individuales** con su texto completo.
    *   Confirma que ya no aparece la frase "2 mensajes nuevos".

> [!TIP]
> Si el teléfono monitoreado es un modelo antiguo, asegúrate de que el interruptor de "Monitoreo" esté encendido en la nueva pantalla de control.
