# Plan de Implementación: Gestión y Limpieza de Dispositivos Enlazados

Este plan introduce la funcionalidad necesaria para gestionar la lista de dispositivos vinculados, permitiendo eliminar dispositivos individuales o limpiar la lista completa desde el panel administrativo.

## Objetivos
1.  **Eliminación Individual**: Permitir a los padres desvincular un teléfono específico si ya no se desea monitorear.
2.  **Limpieza Total**: Opción para borrar todos los tokens generados y empezar de cero.
3.  **Seguridad**: Asegurar que solo el administrador autenticado pueda realizar estas acciones.
4.  **Actualización de UI**: Añadir botones de "Eliminar" en la sección de dispositivos del panel web.

## Cambios Propuestos

### 1. Backend (Node.js)

#### [MODIFY] [deviceController.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/controllers/deviceController.js)
- Añadir función `deleteDevice(token)`: Elimina un dispositivo específico de la base de datos.
- Añadir función `deleteAllDevices()`: Limpia toda la tabla de dispositivos.

#### [MODIFY] [server.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/server.js)
- Registrar nuevas rutas:
    - `DELETE /api/devices/:token` (Individual)
    - `DELETE /api/devices` (Masivo)
- Ambas protegidas por el middleware `requireAdminAuth`.

### 2. Frontend (Panel Web)

#### [MODIFY] [index.html](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/public/index.html)
- Actualizar la función `loadDevices()` para incluir un botón de "Eliminar" (🗑️) al lado de cada token.
- Añadir un botón general de "Limpiar Todos los Dispositivos" en la sección de gestión.
- Implementar las llamadas a la API correspondientes con confirmación previa.

## Plan de Verificación
1.  **Prueba de Desvinculación**: Borrar un dispositivo del panel y verificar que la app Android correspondiente recibe un error "401 No autorizado" al intentar enviar alertas.
2.  **Prueba de Limpieza**: Usar la opción masiva y confirmar que la lista de dispositivos queda vacía.
3.  **Persistencia**: Verificar que las alertas existentes no se borren (ya que están asociadas al token pero no dependen de la existencia del dispositivo en la tabla `devices` para su lectura histórica, aunque se recomienda limpiar alertas antes si se desea un borrado total).

---

**¿Deseas que proceda con la implementación de la limpieza de dispositivos?**
