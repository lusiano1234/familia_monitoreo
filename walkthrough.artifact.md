# Walkthrough - Entrada Manual de Token del Sitio Web

Se ha modificado el flujo de la aplicación para que el usuario ingrese manualmente el token proporcionado por el sitio web.

## Cambios Realizados

### Configuración y Dependencias
*   **[app/build.gradle](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/build.gradle)**: Se añadió la dependencia de Material Components para mejorar la interfaz de usuario.
*   **[themes.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/values/themes.xml)**: Se actualizó el tema base a `Theme.MaterialComponents` para evitar cierres inesperados al usar componentes Material.

### Pantalla de Consentimiento (Entrada de Token)
*   **[activity_consent.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_consent.xml)**: Se incorporó un campo de texto (`TextInputLayout` + `TextInputEditText`) para que el usuario ingrese el token del sitio web.
*   **[ConsentActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/ConsentActivity.kt)**:
    *   Se implementó validación en tiempo real: el botón "Continuar" solo se habilita si el campo del token no está vacío y el checkbox de consentimiento está marcado.
    *   La app ahora guarda el token ingresado por el usuario en lugar de generar uno aleatorio.

### Pantalla de Estado
*   **[StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)**: Continúa mostrando el token guardado para verificación del usuario.

## Verificación Visual

> [!NOTE]
> La interfaz ahora requiere el token antes de permitir la activación del servicio.

![Nueva pantalla de consentimiento con campo de token](/C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/build/tmp/screenshot_consent.png)

## Instrucciones para el Usuario

1.  Abre la aplicación "Monitoreo Familiar".
2.  Escribe o pega el **token que te dio el sitio web** en el campo indicado.
3.  Marca la casilla de consentimiento.
4.  Presiona el botón para continuar y activar los permisos necesarios.
