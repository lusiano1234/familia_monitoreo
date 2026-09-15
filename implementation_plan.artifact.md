# Plan de Implementación: Entrada de Token desde el Sitio Web

Este plan modifica la lógica de identificación para que el usuario ingrese manualmente el token proporcionado por el sitio web, en lugar de generar uno aleatorio en el dispositivo.

## Problemas Identificados
- El flujo actual genera un token en el móvil, pero el usuario necesita ingresar un token que ya tiene del sitio web.
- No hay campo de entrada en la interfaz para este propósito.

## Cambios Propuestos

### 1. Interfaz de Consentimiento y Configuración
Añadiremos un campo de texto para que el usuario pegue o escriba el token del sitio web.

#### [MODIFY] [activity_consent.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_consent.xml)
- Añadir un `EditText` con un hint claro (ej. "Ingresa el token del sitio web").
- Añadir un `TextView` instructivo.

#### [MODIFY] [ConsentActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/ConsentActivity.kt)
- Validar que el campo del token no esté vacío antes de habilitar el botón "Continuar".
- Guardar el valor ingresado por el usuario en `SharedPreferences` ("device_token").

### 2. Pantalla de Estado
Mantendremos la visualización del token para que el usuario pueda confirmar cuál ingresó.

#### [MODIFY] [StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)
- (Opcional) Permitir la edición del token si el usuario se equivocó al ingresarlo inicialmente. Por ahora, nos centraremos en mostrarlo correctamente.

## Plan de Verificación

### Pruebas Automatizadas
- N/A.

### Verificación Manual
1. Abrir la app en la pantalla de consentimiento.
2. Verificar que el botón "Continuar" esté deshabilitado si el token está vacío.
3. Ingresar un token de prueba (ej: "TOKEN-WEB-123").
4. Aceptar el consentimiento y continuar.
5. Verificar en `StatusActivity` que se muestra "TOKEN-WEB-123".
6. Verificar en el Logcat que las alertas (si se disparan) usan el nuevo token.
