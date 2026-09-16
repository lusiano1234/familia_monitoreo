# Plan de Implementación: Corrección de Carga de Reportes (Migración de DB)

Este plan corrige el error que impide cargar los reportes en el panel web. El problema se debe a que la base de datos no añadió automáticamente las nuevas columnas de Batería y Red en la tabla existente.

## Diagnóstico
El comando `CREATE TABLE IF NOT EXISTS` solo funciona cuando la tabla se crea por primera vez. Como la tabla `alerts` ya existía en Render, las columnas `battery_level` y `connection_type` no se crearon, lo que provoca que la consulta de reportes falle (Error 500).

## Cambios Propuestos

### 1. Backend (Base de Datos)

#### [MODIFY] [db.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/db.js)
- Implementar una lógica de **migración forzada**.
- Añadir sentencias `ALTER TABLE alerts ADD COLUMN IF NOT EXISTS ...` para asegurar que las nuevas columnas existan sin importar cuándo se creó la tabla.

### 2. Verificación de Robustez
- Revisar que el panel web maneje correctamente los reportes antiguos que no tienen datos de batería (evitando errores de visualización "null").

## Plan de Verificación
1. **Sincronización**: Al subir este cambio a Render, el servidor ejecutará el comando `ALTER TABLE`.
2. **Carga de Panel**: Entrar al panel web y verificar que la lista de reportes cargue correctamente (incluso si los viejos no tienen íconos).
3. **Nueva Alerta**: Enviar una alerta de prueba desde la app y confirmar que ahora sí aparece con su batería y red.

---

**¿Deseas que aplique la corrección en la base de datos para que los reportes vuelvan a cargar?**
