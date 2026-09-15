require("dotenv").config();
const express = require("express");
const cors = require("cors");
const path = require("path");
const crypto = require("crypto");
const { pool, initDb } = require("./db");

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, "..", "public")));

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || "";

// --- Auth de dispositivos (la app Android manda "Authorization: Bearer <token>") ---
async function requireDeviceAuth(req, res, next) {
  const auth = req.headers.authorization || "";
  const token = auth.startsWith("Bearer ") ? auth.slice(7) : null;
  if (!token) return res.status(401).json({ error: "Falta token de dispositivo" });

  const result = await pool.query(
    "SELECT id, label FROM devices WHERE device_token = $1",
    [token]
  );
  if (result.rowCount === 0) {
    return res.status(401).json({ error: "Token de dispositivo inválido" });
  }
  req.device = result.rows[0];
  next();
}

// --- Auth simple del panel para los padres (contraseña compartida) ---
function requireAdminAuth(req, res, next) {
  if (!ADMIN_PASSWORD) {
    return res.status(500).json({ error: "ADMIN_PASSWORD no configurada en el servidor" });
  }
  const header = req.headers.authorization || "";
  const provided = header.startsWith("Bearer ") ? header.slice(7) : req.query.password;
  if (provided !== ADMIN_PASSWORD) {
    return res.status(401).json({ error: "No autorizado" });
  }
  next();
}

// --- Endpoint que usa AlertUploader.kt ---
app.post("/api/alerts", requireDeviceAuth, async (req, res) => {
  const { sourceApp, category, level, fragment, timestamp } = req.body || {};

  if (!sourceApp || !category || !level || !fragment || !timestamp) {
    return res.status(400).json({ error: "Faltan campos obligatorios" });
  }

  await pool.query(
    `INSERT INTO alerts (device_token, source_app, category, level, fragment, device_timestamp)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [req.headers.authorization.slice(7), sourceApp, category, level, fragment, timestamp]
  );

  // Lógica de notificación para alertas críticas
  if (level === "HIGH") {
    console.log("---------------------------------------------------------");
    console.log("¡NOTIFICACIÓN CRÍTICA ENVIADA A LOS PADRES!");
    console.log(`Dispositivo: ${req.device.label || req.device.id}`);
    console.log(`Aplicación: ${sourceApp}`);
    console.log(`Categoría: ${category}`);
    console.log(`Fragmento Detectado: "${fragment}"`);
    console.log("---------------------------------------------------------");

    // Aquí se integraría Nodemailer para email o Firebase Cloud Messaging para Push
  }

  res.status(201).json({ ok: true });
});

// --- Panel para que los padres vean las alertas ---
app.get("/api/alerts", requireAdminAuth, async (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 50, 200);
  const result = await pool.query(
    `SELECT a.id, a.source_app, a.category, a.level, a.fragment, a.device_timestamp,
            a.received_at, d.label AS device_label
     FROM alerts a
     LEFT JOIN devices d ON d.device_token = a.device_token
     ORDER BY a.received_at DESC
     LIMIT $1`,
    [limit]
  );
  res.json(result.rows);
});

// --- Alta de dispositivos (para generar el token que va en la app Android) ---
app.post("/api/devices", requireAdminAuth, async (req, res) => {
  const { label } = req.body || {};
  const token = crypto.randomBytes(24).toString("hex");
  await pool.query(
    "INSERT INTO devices (device_token, label) VALUES ($1, $2)",
    [token, label || null]
  );
  res.status(201).json({ deviceToken: token, label: label || null });
});

app.get("/api/devices", requireAdminAuth, async (req, res) => {
  const result = await pool.query(
    "SELECT id, device_token, label, created_at FROM devices ORDER BY created_at DESC"
  );
  res.json(result.rows);
});

app.get("/health", (req, res) => res.json({ ok: true }));

const PORT = process.env.PORT || 3000;

initDb()
  .then(() => {
    app.listen(PORT, () => console.log(`Backend escuchando en puerto ${PORT}`));
  })
  .catch((err) => {
    console.error("Error inicializando la base de datos:", err);
    process.exit(1);
  });
