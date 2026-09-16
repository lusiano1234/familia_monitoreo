# Plan de Mejora: "Seguridad Familiar Total"

Este plan detalla las funcionalidades recomendadas para cubrir todos los escenarios de riesgo posibles, transformando la aplicación en una herramienta de protección integral.

## Escenarios Cubiertos
1.  **Riesgo Físico**: Saber dónde está el niño cuando ocurre una alerta.
2.  **Acoso por SMS**: Detectar amenazas fuera de las apps de chat.
3.  **Emergencia Silenciosa**: Permitir al niño pedir ayuda sin que nadie lo note.
4.  **Control de Crisis**: Bloquear el dispositivo remotamente desde el panel web.

---

## 1. Localización en Tiempo Real (Geolocalización)
*   **App**: Capturar coordenadas GPS (Latitud/Longitud) cada vez que se dispare una alerta de riesgo.
*   **Backend**: Almacenar la ubicación vinculada a la alerta.
*   **Panel Web**: Mostrar un mapa con la ubicación exacta de donde se recibió el mensaje peligroso.

## 2. Monitor de SMS y Llamadas del Sistema
*   **App**: Implementar un observador para mensajes de texto (SMS). Los estafadores suelen usar SMS cuando son bloqueados en WhatsApp.
*   **App**: Registrar llamadas perdidas de números desconocidos.

## 3. Comandos Remotos (Socket.io Bidireccional)
*   **Panel Web**: Botón para **"Hacer sonar alarma"** (incluso si está en silencio) para encontrar el teléfono o asustar a un agresor.
*   **Panel Web**: Botón para **"Bloquear Pantalla"** si se detecta una situación de grooming extrema.

## 4. Botón de Pánico Discreto
*   **App**: Un gesto secreto (ej: presionar 5 veces el botón de encendido) que envíe una alerta inmediata al panel con la ubicación actual y una grabación de audio de 15 segundos.

---

## Cambios Técnicos Propuestos

### App Android
*   **[NEW] `LocationHelper.kt`**: Gestión de permisos GPS y obtención de coordenadas.
*   **[NEW] `SmsObserver.kt`**: Monitoreo de la base de datos de mensajes entrantes.
*   **[MODIFY] `NotificationCaptureService.kt`**: Integración con Socket.io para recibir órdenes desde el panel web.

### Backend (Node.js)
*   **[MODIFY] `db.js`**: Añadir columnas `latitude`, `longitude` y `accuracy`.
*   **[MODIFY] `server.js`**: Habilitar el envío de mensajes desde el Panel -> App a través de Sockets.

### Frontend (Panel Web)
*   **[MODIFY] `index.html`**: Integrar la API de **Google Maps** o **Leaflet** para visualizar las ubicaciones.
*   **[MODIFY] `index.html`**: Panel de "Acciones Rápidas" (Alarma, Bloqueo).

---

> [!IMPORTANT]
> La localización en segundo plano en Android 14 requiere que el usuario acepte el permiso "Permitir siempre". Esto es fundamental para que el GPS funcione con la pantalla apagada.

**¿Qué opinas de estas recomendaciones? ¿Deseas que empecemos por la Localización GPS o por los Comandos Remotos?**
