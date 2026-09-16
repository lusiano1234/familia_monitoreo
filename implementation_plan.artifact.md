# Plan de Implementación: Compatibilidad Universal (Motorola, Xiaomi, Samsung, Vivo)

Este plan introduce un sistema de asistencia inteligente que detecta la marca del teléfono y guía al usuario para configurar los ajustes específicos que impiden que el sistema "mate" la aplicación en segundo plano.

## Objetivos
1.  **Detección de Marca**: Identificar automáticamente el fabricante del dispositivo.
2.  **Asistente Multi-Marca**: Mostrar guías personalizadas para los menús críticos de cada fabricante.
3.  **Persistencia Robusta**: Implementar técnicas de "revinculación" forzada para despertar el servicio si el sistema lo duerme.
4.  **Avisos de "Ajustes Restringidos"**: Facilitar el desbloqueo de los 3 puntos (⋮) en Android 13/14 para todas las marcas.

---

## 1. Guías Específicas por Fabricante

### Motorola (Edge/Moto G)
*   **Ajuste Crítico**: Rendimiento -> Gestión de aplicaciones -> **Permitir siempre**.
*   **Batería**: Desactivar "Mejorar batería mientras está inactivo".

### Xiaomi (MIUI / HyperOS)
*   **Ajuste Crítico**: Activar **Inicio automático**.
*   **Batería**: Ahorro de batería -> **Sin restricciones**.
*   **Otros**: Permitir "Mostrar ventanas emergentes en segundo plano".

### Samsung (One UI)
*   **Ajuste Crítico**: Límites de uso de fondo -> **Aplicaciones nunca inactivas**.
*   **Batería**: Optimizar uso de batería -> **No optimizar**.

### Vivo (Funtouch OS)
*   **Ajuste Crítico**: Batería -> Gestión de consumo de energía en segundo plano -> **No restringir**.
*   **Inicio**: Activar "Inicio automático".

---

## 2. Cambios Propuestos en la App

### [MODIFY] [StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)
*   Implementar `detectBrand()` para mostrar el logo y el botón de ayuda correspondiente.
*   Crear un sistema de diálogos dinámicos que cambien según el fabricante detectado.

### [MODIFY] [NotificationCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/NotificationCaptureService.kt)
*   **Watchdog (Perro guardián)**: Implementar una técnica de auto-reinicio si el servicio es desconectado por el sistema (vía `requestRebind`).

### [MODIFY] [res/layout/activity_status.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_status.xml)
*   Diseñar un área de "Configuración de Marca" que llame la atención del usuario con un diseño intuitivo.

---

## 3. Plan de Verificación
1.  **Validación de Marca**: Abrir la app en los 4 modelos y confirmar que muestra la guía correcta.
2.  **Prueba de "Sueño Profundo"**: Bloquear cada teléfono por 30 minutos y verificar que los mensajes siguen llegando al panel.
3.  **Facilidad de Uso**: Confirmar que los botones llevan a las pantallas de ajustes correctas de cada marca.

**¿Deseas que proceda con la implementación de esta compatibilidad universal para cubrir Motorola, Xiaomi, Samsung y Vivo?**
