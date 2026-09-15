# Backend de Monitoreo Familiar

Recibe las alertas que manda `AlertUploader.kt` desde la app Android y las
guarda en Postgres. Incluye un panel web simple (`/`) para que los padres
vean las alertas y generen tokens de dispositivo.

## Qué incluye

- `POST /api/alerts` — la app Android manda alertas acá (autenticado con el
  token del dispositivo vía `Authorization: Bearer <token>`).
- `GET /api/alerts` — lista de alertas, protegido con `ADMIN_PASSWORD`.
- `POST /api/devices` — genera un token nuevo para vincular un dispositivo.
- `GET /api/devices` — lista los dispositivos ya registrados.
- `GET /health` — chequeo de salud.
- `/` — panel HTML simple para ver alertas y generar tokens sin usar curl.

## Desplegar en Render (plan free)

1. Subí esta carpeta a un repo de GitHub (o GitLab).
2. En Render: **New +** → **Blueprint**, y apuntá al repo. Render va a leer
   `render.yaml` y crear automáticamente el web service y la base Postgres.
3. Cuando te lo pida, cargá la variable `ADMIN_PASSWORD` (elegí una
   contraseña fuerte — es la que usan los padres para ver el panel).
4. Al terminar el deploy vas a tener una URL tipo
   `https://family-monitor-backend.onrender.com`.
5. Abrí esa URL en el navegador, poné la contraseña, y generá un token con
   el botón **Generar token nuevo**.
6. Copiá ese token en la app Android (reemplazando `"TOKEN_DE_PRUEBA"` en
   `AlertUploader.kt`, o donde la app lo pida al usuario si ya tenés una
   pantalla para eso), y actualizá `BASE_URL` en `AlertUploader.kt` a:
   ```
   https://TU-URL-DE-RENDER.onrender.com/api/alerts
   ```

### Importante sobre el plan free de Render

- El web service **se duerme a los 15 minutos de inactividad** y tarda
  30-60 segundos en "despertar" cuando llega la primera alerta después de
  eso. Para un prototipo está bien; para depender de esto en una situación
  real, conviene pasar el web service al plan **Starter (~$7/mes)**, que
  queda siempre despierto — cambiás el plan desde el dashboard de Render
  sin tocar código.
- La base Postgres free **expira si no la renovás** (Render avisa antes).
  Para no perder el historial de alertas, o programá el upgrade a tiempo,
  o hacé un export periódico.

## Correr localmente

```bash
cp .env.example .env
# editá .env con tu propia base Postgres local o una de prueba
npm install
npm start
```

Servidor en `http://localhost:3000`.

## Registrar un dispositivo por línea de comandos (alternativa al panel)

```bash
curl -X POST https://TU-URL.onrender.com/api/devices \
  -H "Authorization: Bearer TU_ADMIN_PASSWORD" \
  -H "Content-Type: application/json" \
  -d '{"label": "Celular de Juli"}'
```

Te devuelve un `deviceToken` — ese es el que va en la app Android.

## Próximos pasos posibles

- Reemplazar el panel de "ver alertas manualmente" por notificaciones push
  o email/SMS automáticos apenas llega una alerta de nivel alto.
- Agregar rotación/expiración de tokens de dispositivo.
- Exportar alertas viejas antes de que expire la base free.
