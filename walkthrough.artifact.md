# Walkthrough - Generación de Token de Dispositivo

Se ha implementado la lógica para generar, guardar y mostrar un token único de dispositivo, permitiendo la autenticación con el backend.

## Cambios Realizados

### Generación de Identidad
*   **[ConsentActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/ConsentActivity.kt)**: Al momento de aceptar el consentimiento, la app genera un UUID aleatorio y lo guarda en `SharedPreferences` bajo la clave `device_token`.

### Interfaz de Usuario
*   **[activity_status.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_status.xml)**: Se añadió una sección de "Token del dispositivo" que muestra el identificador en un formato legible (monospace) y un botón para copiarlo.
*   **[StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)**: Se programó la lógica para leer el token guardado y copiarlo al portapapeles del sistema al presionar el botón.

## Verificación

1.  **Despliegue**: La aplicación se compiló y desplegó correctamente en el dispositivo Samsung.
2.  **Visualización**: La pantalla de estado ahora muestra el token generado automáticamente.
3.  **Captura de Pantalla**:
![Token generado y visible en la pantalla de estado](/C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/build/tmp/screenshot.png)

> [!IMPORTANT]
> **Tu Token de Dispositivo actual es:** `c37bae76-784e-4329-8038-750f15bea185`.
> Debes usar este identificador en tu backend para vincular los reportes de este móvil.

> [!TIP]
> Puedes usar el botón **"COPIAR TOKEN"** dentro de la app para obtenerlo fácilmente.
