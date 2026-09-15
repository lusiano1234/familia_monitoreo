# Plan de Implementación: Generación de Token de Dispositivo

Este plan aborda la falta de un token de identificación para el backend. Implementaremos una lógica para generar un identificador único (UUID) la primera vez que se otorga el consentimiento y lo mostraremos en la pantalla de estado.

## Problemas Identificados
- El backend requiere un token en los encabezados de autorización.
- La aplicación móvil intenta leer `device_token` de `SharedPreferences` ("device"), pero este valor nunca se genera ni se guarda.

## Cambios Propuestos

### 1. Generación de Token en `ConsentActivity`
Generaremos un UUID aleatorio cuando el usuario presione el botón de "Continuar" después de aceptar los términos.

#### [MODIFY] [ConsentActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/ConsentActivity.kt)
- Añadir lógica para generar un UUID.
- Guardarlo en `getSharedPreferences("device", MODE_PRIVATE)`.

### 2. Visualización en `StatusActivity`
Mostraremos el token generado en la pantalla de estado para que el usuario pueda copiarlo o verificarlo si el backend lo solicita manualmente durante el registro.

#### [MODIFY] [activity_status.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_status.xml)
- Añadir un `TextView` para mostrar el token.
- Añadir un botón opcional para copiar el token al portapapeles.

#### [MODIFY] [StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)
- Leer el `device_token` y mostrarlo en el nuevo `TextView`.
- Implementar la funcionalidad de copiar al portapapeles.

## Plan de Verificación

### Pruebas Automatizadas
- N/A (Cambios principalmente de UI y lógica simple de SharedPreferences).

### Verificación Manual
1. Abrir la app y aceptar el consentimiento.
2. Verificar en el Logcat que `AlertUploader` ahora usa un UUID en lugar de "TOKEN_DE_PRUEBA".
3. Navegar a la pantalla de estado y confirmar que el token es visible y se puede copiar.
