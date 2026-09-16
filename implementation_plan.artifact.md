# Plan de Implementación: Detalle Completo de Mensajes

Este plan modifica el motor de riesgo y el panel web para que, en lugar de mostrar solo la palabra "prohibida", se muestre el **mensaje completo** recibido por el niño, permitiendo a los padres entender todo el contexto.

## Objetivos
1.  **Contexto Total**: Capturar y enviar el mensaje íntegro (remitente y texto) cuando se detecte un riesgo.
2.  **Mejora Visual**: Actualizar el panel web para que el detalle sea legible y profesional.

## Cambios Propuestos

### 1. App Android

#### [MODIFY] [RiskEngine.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/RiskEngine.kt)
- Cambiar la lógica de `evaluate` para que, cuando encuentre una coincidencia, devuelva el **texto original completo** en lugar de solo el fragmento que coincidió con la regla.

#### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
- Asegurar que el formato enviado sea claro (ej: `Nombre: Mensaje`).

### 2. Frontend (Panel Web)

#### [MODIFY] [index.html](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/family-monitor-backend/public/index.html)
- Ajustar el diseño de las tarjetas para que el mensaje completo se vea en una fuente más clara y grande.
- Implementar una separación visual entre el remitente y el contenido si el formato lo permite.

## Plan de Verificación
1.  **Prueba de Contexto**: Enviar un WhatsApp largo que incluya una palabra de riesgo (ej: "Hola hijo, como estas, decime **donde vives**").
2.  **Validación en Panel**: Confirmar que en el panel web aparece la frase completa "Hola hijo... donde vives" y no solo "donde vives".

---

**¿Deseas que proceda a mostrar el detalle completo de los mensajes?**
