# Plan de Implementación: Profesionalización del Backend (Monitor Familiar)

Este plan transforma el backend actual de un prototipo básico a una aplicación robusta, segura y escalable, siguiendo las mejores prácticas de la industria.

## Mejoras Propuestas

### 1. Arquitectura Modular
- **Reorganización**: Separar `server.js` en carpetas:
    - `/routes`: Definición de endpoints.
    - `/controllers`: Lógica de negocio.
    - `/middlewares`: Seguridad, validación y errores.
- **Beneficio**: Facilita el mantenimiento y la expansión futura (ej: añadir usuarios, reportes PDF, etc.).

### 2. Actualizaciones en Tiempo Real (WebSockets)
- **Tecnología**: Integrar **Socket.io**.
- **Cambio**: Cuando la app Android envíe una alerta (`POST /api/alerts`), el servidor la emitirá instantáneamente al panel web de los padres.
- **Beneficio**: Los padres no tendrán que refrescar la página manualmente para ver si hay una alerta nueva.

### 3. Seguridad Avanzada
- **JWT (JSON Web Tokens)**: Implementar un flujo de login real para el panel. En lugar de mandar la contraseña en cada request, el admin se loguea una vez y recibe un token firmado.
- **Rate Limiting**: Limitar la cantidad de peticiones desde una misma IP para evitar ataques de fuerza bruta o saturación.
- **Validación de Datos**: Usar esquemas para asegurar que los datos que vienen de la app Android son válidos antes de procesarlos.

### 4. Notificaciones Externas (E-mail)
- **Tecnología**: Preparar la integración con **Nodemailer**.
- **Lógica**: Si una alerta es `HIGH`, el servidor intentará enviar un correo automático a la dirección de los padres configurada.

### 5. Observabilidad y Errores
- **Logging**: Integrar **Morgan** para ver todas las peticiones en los logs de Render de forma clara.
- **Manejo Global de Errores**: Middleware centralizado para capturar cualquier fallo y responder con un formato JSON profesional en lugar de exponer trazas de código.

## Cambios en Archivos

### Backend
#### [NEW] [authController.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/controllers/authController.js)
- Manejo de login y generación de JWT.
#### [NEW] [alertController.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/controllers/alertController.js)
- Lógica de guardado de alertas y emisión por WebSockets.
#### [NEW] [middleware.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/middlewares/auth.js)
- Verificación de JWT y autenticación de dispositivos.
#### [MODIFY] [server.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/server.js)
- Configuración de Socket.io y orquestación de rutas.
#### [MODIFY] [index.html](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/public/index.html)
- Actualización para soportar WebSockets y manejo de sesión con JWT.

## Plan de Verificación
1.  **Prueba de Socket**: Enviar una alerta desde la app y ver cómo aparece en el panel sin refrescar.
2.  **Prueba de Seguridad**: Intentar acceder al panel sin el token JWT.
3.  **Prueba de Estructura**: Verificar que el servidor arranca correctamente con la nueva arquitectura.

---

> [!QUESTION]
> ¿Tienes algún servicio de e-mail preferido (ej: Gmail, SendGrid) o prefieres que deje la lógica genérica lista para que solo pongas las credenciales?
