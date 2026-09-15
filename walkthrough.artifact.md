# Walkthrough - Expansión de Reglas y Detección de Desconocidos

Se han implementado mejoras significativas en el motor de riesgo y en la capacidad de detección de la aplicación, incluyendo la identificación de contactos no registrados y la ampliación de patrones de mensajes peligrosos.

## Cambios Realizados

### Motor de Riesgo (Risk Engine)
*   **[RiskEngine.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/RiskEngine.kt)**: Se añadieron nuevas categorías de detección:
    *   **Grooming**: Detección de preguntas sobre ubicación o soledad.
    *   **Sextorsión**: Patrones de amenazas con material sensible.
    *   **Citas Sospechosas**: Invitaciones a encuentros privados.
    *   **Robo de Cuenta**: Solicitudes de códigos de verificación o SMS.

### Detección de Contactos Desconocidos
*   **[ContactHelper.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/ContactHelper.kt)**: Nuevo componente que utiliza el `ContentResolver` del sistema para verificar si un remitente (nombre o número) existe en la agenda del teléfono.
*   **[NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)**:
    *   Ahora cruza cada notificación con la agenda. Si el remitente es desconocido, se envía una alerta automática de nivel `MEDIUM`.
    *   Se amplió el monitoreo para incluir las aplicaciones de **Teléfono (Llamadas)**.

### Gestión de Permisos
*   **[AndroidManifest.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/AndroidManifest.xml)**: Se añadió el permiso `READ_CONTACTS`.
*   **[StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)**: Se añadió una sección de "Permisos adicionales" para solicitar el acceso a contactos de forma transparente.

## Verificación Visual

![Pantalla de estado con detección de desconocidos activa](/C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/build/tmp/screenshot_status.png)

## Instrucciones para el Usuario

1.  **Permitir Contactos**: En la pantalla de estado de la app, presiona el botón **"Permitir detectar desconocidos"**. Esto es vital para que la app sepa quién es un contacto de confianza y quién no.
2.  **Prueba de Llamada**: Puedes probar llamando desde un número que no tengas guardado; verás que el sistema genera una alerta en el panel web.
3.  **Seguridad**: Recuerda que la app solo envía al servidor el nombre/número del desconocido y el fragmento del mensaje, manteniendo la privacidad de tus contactos conocidos.
