# Plan de Implementación: Solución Definitiva para Permisos Bloqueados (Android 13/14)

Este plan aborda de manera integral el bloqueo de "Ajustes restringidos" en Android 13 y 14, proporcionando una interfaz de asistencia que guía al usuario para habilitar el monitoreo de mensajes en cualquier teléfono moderno.

## Diagnóstico Técnico
Android 13+ introdujo una protección que deshabilita el "Acceso a notificaciones" para apps instaladas por APK. El interruptor aparece "gris" y dice "Ajuste restringido". La única forma de habilitarlo es a través de un menú oculto en la pantalla de **Información de la aplicación**.

## Objetivos
1.  **Detección Automática**: Avisar al usuario con un banner rojo si el sistema no tiene permiso para leer mensajes.
2.  **Guía de Desbloqueo (3 Puntos)**: Mostrar instrucciones visuales claras sobre cómo habilitar los "Ajustes restringidos".
3.  **Accesos Directos Inteligentes**:
    *   Botón para ir a **Ajustes de Notificaciones** (donde está el interruptor).
    *   Botón para ir a **Información de la App** (donde están los 3 puntos para desbloquear).
4.  **Optimización de Batería**: Guía para desactivar el ahorro de energía que detiene el monitoreo.

## Cambios Propuestos

### 1. App Android

#### [MODIFY] [StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)
- Implementar `isNotificationServiceEnabled()` para detectar el estado real del permiso.
- Añadir lógica para mostrar un diálogo de "Asistente de Configuración" si el permiso está bloqueado.
- Añadir función `openAppInfo()` para llevar al usuario directamente al menú de los 3 puntos.

#### [MODIFY] [res/layout/activity_status.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_status.xml)
- Añadir un **CardView de Alerta** que solo aparezca cuando falten permisos.
- Incluir un botón de "Cómo desbloquear permisos" con un diseño llamativo.

### 2. Guía Visual (Diálogo de Ayuda)
- Crear un diálogo interactivo que explique los 3 pasos:
    1.  Abrir "Información de la aplicación".
    2.  Tocar los **3 puntos (⋮)** arriba a la derecha.
    3.  Elegir **"Permitir ajustes restringidos"**.

## Plan de Verificación
1.  **Sideload Test**: Instalar la app en un teléfono con Android 13/14.
2.  **Validación de Banner**: Confirmar que el banner de error aparece si el interruptor está bloqueado.
3.  **Flujo de Desbloqueo**: Seguir la guía de los 3 puntos y confirmar que el interruptor se vuelve habilitable.

---

> [!IMPORTANT]
> Sin este cambio, los usuarios de teléfonos nuevos no podrán activar el monitoreo por más que intenten mover el interruptor. Esta es la única solución técnica permitida por Android.

**¿Deseas que proceda con este asistente de permisos avanzados para cubrir todos los teléfonos?**
