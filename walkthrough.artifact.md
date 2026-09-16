# Walkthrough - Restauración del Sistema en Tiempo Real

Se ha completado la restauración masiva de las funcionalidades profesionales. El sistema ahora permite el monitoreo en vivo y tiene un blindaje reforzado contra extorsiones.

## Mejoras Restauradas

### Tiempo Real (WebSockets)
*   Se integró **Socket.io** en el backend y frontend.
*   Las alertas ahora aparecen **automáticamente** en el panel web sin necesidad de recargar la página.
*   Se añadió un indicador visual ("● EN VIVO") para confirmar la conexión activa.

### Seguridad Profesional
*   **JWT (Tokens)**: El panel web ahora usa una sesión segura y persistente.
*   **Modularización**: El código del servidor está organizado en Controladores y Middlewares, garantizando estabilidad.
*   **Protección de Login**: Se limitó el número de intentos de acceso para evitar ataques.

### Blindaje Infantil Reforzado
*   **Español Neutro**: Las reglas de riesgo se adaptaron para detectar extorsiones sin importar el regionalismo.
*   **Normalización Inteligente**: El sistema ahora ignora acentos y mayúsculas, haciendo que sea imposible evadir el filtro con tildes (ej: "borrá" es detectado igual que "borra").

## Cómo usar el nuevo sistema

1.  **Sube los cambios**: Al subir esto a Render, el servidor se actualizará automáticamente.
2.  **Inicia Sesión**: Entra al panel con tu contraseña. La sesión quedará guardada en tu navegador.
3.  **Monitorea en Vivo**: Deja la ventana abierta. En cuanto el niño reciba un mensaje peligroso, la tarjeta aparecerá en tu pantalla instantáneamente en rojo.

> [!TIP]
> **Prueba de Fuego**: Envía un WhatsApp con `"borra los mensajes es un secreto"`. Verás que la alerta aparece sola en el panel en menos de un segundo.
