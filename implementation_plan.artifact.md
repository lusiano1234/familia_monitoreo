# Plan de Implementación: Expansión de Reglas y Detección de Desconocidos

Este plan expande las capacidades del motor de riesgo y añade la detección de interacciones (mensajes y llamadas) con contactos que no están en la agenda del teléfono.

## User Review Required

> [!IMPORTANT]
> **Permisos de Contactos**: Para detectar si un contacto es "desconocido", la aplicación ahora requerirá el permiso de **Leer Contactos** (`READ_CONTACTS`). Esto debe ser aceptado por el usuario en la pantalla de configuración.

## Cambios Propuestos

### 1. Expansión de Palabras Clave

#### [MODIFY] [RiskEngine.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/RiskEngine.kt)
- Añadir nuevas categorías de riesgo:
    - **Grooming**: "donde vivis", "estas solo", "pasame tu direccion", "no le cuentes a nadie".
    - **Sextorsión**: "tengo tus fotos", "voy a publicar el video", "borra el chat".
    - **Citas Sospechosas**: "encontremonos", "te paso a buscar", "veni a mi casa".
    - **Robo de Cuenta**: "pasame el codigo", "llego un SMS", "validar cuenta".

### 2. Detección de Contactos Desconocidos

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/AndroidManifest.xml)
- Añadir `<uses-permission android:name="android.permission.READ_CONTACTS" />`.

#### [NEW] [ContactHelper.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/ContactHelper.kt)
- Implementar función `isContactUnknown(context, nameOrNumber)` que consulta el `ContentResolver` para verificar si el remitente existe en los contactos del sistema.

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- Extraer el nombre del remitente de los extras de la notificación.
- Si el remitente es desconocido, generar una alerta automática de nivel `MEDIUM` con la categoría `contacto_desconocido`.

### 3. Monitoreo de Llamadas

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- Añadir paquetes de telefonía (Dialer) a la lista de monitoreo.
- Detectar notificaciones de categoría `Notification.CATEGORY_CALL`.
- Verificar si el número de la llamada entrante es desconocido y avisar.

### 4. Solicitud de Permisos en UI

#### [MODIFY] [StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)
- Añadir un botón o chequeo para solicitar el permiso de contactos si aún no ha sido otorgado.

## Plan de Verificación

### Pruebas Manuales
1.  **Nuevas palabras**: Enviar un WhatsApp con "pasame tu dirección" y verificar la alerta.
2.  **Contacto desconocido**: Enviar un mensaje desde un número que NO esté en los contactos del teléfono y verificar que llegue el aviso de "Contacto desconocido".
3.  **Llamada desconocida**: Realizar una llamada desde un número no registrado y verificar el aviso en el backend.
