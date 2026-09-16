# Tareas: Certeza de Captura y Seguridad de Sesión

- [x] Implementar Flag de Sesión Global en `PinActivity`
- [x] Implementar Bloqueo Automático en `StatusActivity` (onStop)
- [x] Expandir Apps Monitoreadas (Telecom y SMS) en `NotificationCaptureService`
- [x] Refactorizar `onNotificationPosted` para iteración manual de `EXTRA_MESSAGES` (Captura Profunda)
- [x] Implementar sistema de reintentos (Retry) en `AlertUploader`
- [x] Añadir Logs de Auditoría detallados
- [/] Verificación
    - [ ] Validar re-bloqueo al salir de la app
    - [ ] Validar captura de mensajes agrupados (WhatsApp)
    - [ ] Analizar logs en teléfonos que no reportan
