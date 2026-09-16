# Plan de Implementación: Restauración y Profesionalización (Real-Time & Blindaje)

Este plan restaura las funcionalidades profesionales perdidas y asegura que los reportes se actualicen en tiempo real mediante WebSockets, además de blindar el motor de riesgo contra extorsiones.

## Objetivos
1.  **Tiempo Real**: Integrar `Socket.io` para que las alertas aparezcan en el panel sin refrescar.
2.  **Arquitectura Profesional**: Organizar el backend en Controladores, Middlewares y Rutas.
3.  **Seguridad**: Implementar JWT para el panel administrativo y proteger el login contra ataques.
4.  **Notificaciones**: Configurar la vía de e-mail mediante SendGrid (inmune a bloqueos de Render).
5.  **Blindaje de Riesgo**: Reforzar la app Android con reglas neutras contra extorsión y grooming.

## Cambios Propuestos

### 1. Backend (Node.js)

#### [MODIFY] [package.json](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/package.json)
- Añadir dependencias: `jsonwebtoken`, `socket.io`, `express-rate-limit`, `helmet`, `morgan`, `@sendgrid/mail`, `bcryptjs`.

#### [NEW] [auth.js (Middleware)](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/middlewares/auth.js)
- Validación de JWT para padres y Device Token para la app.

#### [NEW] [alertController.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/controllers/alertController.js)
- Lógica de alertas: Guardar en DB -> Emitir por Socket -> Notificar por SendGrid.

#### [NEW] [deviceController.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/controllers/deviceController.js)
- Gestión de tokens de vinculación.

#### [NEW] [authController.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/controllers/authController.js)
- Login administrativo con generación de tokens JWT.

#### [MODIFY] [server.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/server.js)
- Reescritura completa para integrar Socket.io y la nueva estructura modular.

### 2. Frontend (Panel Web)

#### [MODIFY] [index.html](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/public/index.html)
- Integrar cliente de Socket.io (desde CDN para máxima compatibilidad).
- Sistema de login real con persistencia de sesión en `localStorage`.
- Interfaz moderna con animaciones para alertas nuevas.

### 3. App Android (Protección)

#### [MODIFY] [RiskEngine.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/RiskEngine.kt)
- Expandir categorías (Extorsión Digital, Aislamiento, Difusión).
- Implementar normalización de texto (ignora acentos y mayúsculas).
- Neutralizar idioma (español neutro).

## Plan de Verificación
1.  **Conectividad**: Verificar que el panel muestra "MONITOREO EN VIVO".
2.  **Tiempo Real**: Enviar una alerta desde la app y confirmar su aparición instantánea.
3.  **Seguridad**: Validar que los endpoints administrativos requieren el token JWT.

---

**¿Deseas que proceda con la restauración masiva para arreglar el tiempo real?**
