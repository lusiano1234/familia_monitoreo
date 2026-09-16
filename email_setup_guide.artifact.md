# Guía: Configuración de Notificaciones por E-mail

Para que el backend pueda enviar correos electrónicos automáticamente cuando se detecte un riesgo alto, debes configurar las credenciales en tu panel de Render.

## 1. Obtener Contraseña de Aplicación (Google/Gmail)
Google no permite usar tu contraseña normal para enviar correos desde aplicaciones externas. Debes generar una **Contraseña de Aplicación**:

1.  Ve a tu [Cuenta de Google](https://myaccount.google.com/).
2.  Entra en la sección de **Seguridad**.
3.  Asegúrate de tener activa la **Verificación en dos pasos**.
4.  Busca **"Contraseñas de aplicaciones"** (puedes usar el buscador arriba).
5.  Escribe un nombre (ej: "Monitor Familiar") y dale a **Crear**.
6.  Copia el código de 16 caracteres que aparece (guárdalo, no volverá a aparecer).

## 2. Configurar Variables en Render
1.  Entra al Dashboard de [Render](https://dashboard.render.com/).
2.  Selecciona tu servicio web `family-monitor-backend`.
3.  En el menú lateral, haz clic en **Environment**.
4.  Haz clic en **Add Environment Variable** y añade las siguientes tres:

| Key | Valor |
| :--- | :--- |
| `SMTP_USER` | Tu dirección de Gmail (ej: `padre@gmail.com`). |
| `SMTP_PASS` | El código de 16 caracteres que copiaste de Google. |
| `PARENT_EMAIL` | El correo donde quieres recibir los avisos (puede ser el mismo). |

5.  Haz clic en **Save Changes**. Render reiniciará el servidor automáticamente.

## 3. Verificación
Una vez guardado, haz una prueba enviando un mensaje de riesgo alto (ej: `"donde vivis"`) desde WhatsApp.
- Deberías ver la alerta en el panel web (Live).
- Deberías recibir un e-mail en unos segundos con el asunto `⚠️ ALERTA CRÍTICA`.

> [!CAUTION]
> **Privacidad**: Nunca compartas el `SMTP_PASS` con nadie, ya que da acceso parcial a tu cuenta para enviar correos en tu nombre.
