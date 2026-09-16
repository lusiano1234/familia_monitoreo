# Walkthrough - Gestión de Seguridad y Configuración Avanzada

Se ha implementado un sistema de control centralizado dentro de la app que permite a los padres gestionar la protección con la misma contraseña del panel administrativo.

## Mejoras Realizadas

### Control Maestro de Monitoreo
*   **Interruptor (Switch)**: Se añadió un interruptor en la pantalla de estado para pausar o reanudar el monitoreo de mensajes instantáneamente.
*   **Inteligencia de Servicio**: El `NotificationCaptureService` ahora consulta este estado antes de procesar cualquier notificación. Si el monitoreo está en "Pausa", la app ignora los mensajes para respetar la privacidad o el ahorro de batería.

### Gestión de Identidad y Vinculación
*   **Edición de Token**: El token de vinculación ya no es estático. Ahora puedes editarlo directamente en la app y guardarlo, permitiendo cambiar el dispositivo de panel sin reinstalar la app.
*   **Clave Unificada**: El acceso a la app ahora requiere la **Contraseña Administrativa** del panel web (que se configura en el primer inicio), eliminando el PIN `1234`.

### Control de Blindaje Anti-Borrado
*   **Desactivación Protegida**: Se añadió un botón dinámico para gestionar el Administrador de Dispositivo.
    *   Si está apagado: Te permite activar el blindaje anti-borrado.
    *   Si está encendido: Te permite **desactivar la protección** con un solo toque (tras haber ingresado con tu clave), facilitando la desinstalación legal por parte de los padres.

### Solución de Errores Críticos
*   **Corrección de Cierre Inesperado**: Se actualizó el tema de la aplicación a `Material3` para asegurar la compatibilidad con los nuevos componentes de interfaz (Switch y campos de texto).
*   **Corrección de Base de Datos**: Se implementó una migración automática en el servidor para que el panel web no falle al buscar datos de batería y red.

## Instrucciones de Uso Final

1.  **Configuración Inicial**: Al abrir la app por primera vez tras esta actualización, te pedirá ingresar tu contraseña del panel web y el token.
2.  **Acceso Seguro**: Cada vez que entres a los ajustes ("Servicio de Sincronización"), pon tu clave administrativa.
3.  **Limpiar Historial**: Entra al panel web y usa el botón rojo **"Limpiar Reportes"** para empezar con una lista vacía.
4.  **Verificación Final**: Pulsa **"ENVIAR SEÑAL DE PRUEBA"** en la app para confirmar que todo llega al panel con batería y red.

> [!SUCCESS]
> El sistema está ahora totalmente blindado, es discreto y permite una gestión profesional desde la propia aplicación.
