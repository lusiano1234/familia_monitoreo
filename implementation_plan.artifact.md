# Plan de Diagnóstico: Fallo en Actualización de Reportes

El objetivo es identificar por qué el panel web (EN VIVO) no muestra alertas nuevas a pesar de que el servicio está conectado.

## Diagnóstico Técnico
Tras revisar los logs del servidor, notamos que el panel web se conecta (`Panel conectado: ...`), pero **no hay rastro de peticiones `POST /api/alerts`**. Esto indica que el teléfono no está logrando enviar los datos al servidor.

## Cambios Propuestos para Diagnóstico

### 1. Visibilidad en el Teléfono (App Android)
Añadiremos señales visuales en el teléfono para saber si la app está detectando el riesgo antes de intentar enviarlo.

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- Añadir un `Toast` que diga "Riesgo detectado: [categoría]" para confirmar que el motor de reglas funciona.
- Añadir logs de "Iniciando proceso de envío".

#### [MODIFY] [AlertUploader.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/AlertUploader.kt)
- Añadir logs del resultado exacto del intento de red (ej: "Error de certificado", "Timeout", "DNS Error").

### 2. Visibilidad en el Panel (Backend)
Aseguraremos que el servidor reporte cada intento de conexión de la app.

#### [MODIFY] [middlewares/auth.js](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/src/middlewares/auth.js)
- Añadir un log: `console.log("[AUTH] Intento de dispositivo con token: ...")`.

## Plan de Verificación
1. **Paso 1**: Desplegar estos cambios.
2. **Paso 2**: Enviar el mensaje de WhatsApp `"borra los mensajes"`.
3. **Paso 3**:
    - ¿Apareció el mensaje negro (Toast) en el teléfono? -> El problema es de red/backend.
    - ¿No apareció nada? -> El servicio de captura no está leyendo WhatsApp (posible permiso desactivado).

---

**¿Procedo con este diagnóstico para encontrar la raíz del problema?**
