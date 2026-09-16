# Plan de Implementación: Contexto de Seguridad 360°

Este plan expande la información recolectada en cada alerta para incluir el estado físico del dispositivo (Batería y Conexión), facilitando a los padres una mejor toma de decisiones.

## Objetivos
1.  **Monitoreo de Energía**: Capturar el nivel de batería del teléfono del niño en cada alerta.
2.  **Estado de Red**: Identificar si la alerta se envió vía Wi-Fi o Datos móviles.
3.  **Visualización Inteligente**: Mostrar estos indicadores con íconos en el panel web.
4.  **Persistencia**: Actualizar la base de datos para almacenar esta nueva información.

## Cambios Propuestos

### 1. App Android

#### [NEW] [DeviceStateHelper.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/DeviceStateHelper.kt)
- Crear una utilidad para obtener el nivel de batería (`BatteryManager`) y el tipo de conexión activa (`ConnectivityManager`).

#### [MODIFY] [AlertUploader.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/AlertUploader.kt)
- Expandir la función `sendAlert` para aceptar los parámetros `battery` y `connection`.
- Incluir estos campos en el JSON enviado al servidor.

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- Antes de enviar una alerta, usar `DeviceStateHelper` para adjuntar los datos del sistema.

### 2. Backend (Node.js)

#### [MODIFY] [db.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/db.js)
- Añadir columnas a la tabla `alerts`:
    - `battery_level` (INTEGER)
    - `connection_type` (TEXT)

#### [MODIFY] [controllers/alertController.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/controllers/alertController.js)
- Actualizar la lógica de inserción para guardar los nuevos campos.
- Asegurar que el objeto emitido por Socket.io incluya estos datos.

### 3. Frontend (Panel Web)

#### [MODIFY] [index.html](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/public/index.html)
- Actualizar la función `renderAlert` para mostrar íconos de batería y red.
- Implementar colores dinámicos (ej: batería en rojo si es < 15%).

## Plan de Verificación
1.  **Prueba de Energía**: Enviar una alerta y confirmar en el panel que el nivel de batería coincide con el del teléfono.
2.  **Prueba de Red**: Cambiar el teléfono a "Solo Datos" y verificar que el panel reporta "LTE/4G/5G".

---

> [!CAUTION]
> Para aplicar el cambio en la base de datos, el servidor se reiniciará brevemente. Asegúrate de que no haya reportes críticos en proceso.

**¿Deseas que proceda con la recolección de información extendida?**
