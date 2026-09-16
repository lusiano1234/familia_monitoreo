const { pool } = require("../db");
const sgMail = require("@sendgrid/mail");

if (process.env.SENDGRID_API_KEY) {
  sgMail.setApiKey(process.env.SENDGRID_API_KEY);
}

async function createAlert(req, res, io) {
  const { sourceApp, category, level, fragment, timestamp } = req.body || {};

  if (!sourceApp || !category || !level || !fragment || !timestamp) {
    return res.status(400).json({ error: "Faltan campos obligatorios" });
  }

  try {
    const result = await pool.query(
      `INSERT INTO alerts (device_token, source_app, category, level, fragment, device_timestamp)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING id, received_at`,
      [req.deviceToken, sourceApp, category, level, fragment, timestamp]
    );

    const newAlert = {
      id: result.rows[0].id,
      source_app: sourceApp,
      category,
      level,
      fragment,
      device_timestamp: timestamp,
      received_at: result.rows[0].received_at,
      device_label: req.device.label || "Sin nombre"
    };

    if (io) {
      io.emit("new_alert", newAlert);
    }

    if (level === "HIGH") {
      sendEmailNotification(newAlert);
    }

    res.status(201).json({ ok: true });
  } catch (err) {
    console.error("Error guardando alerta:", err);
    res.status(500).json({ error: "Error interno" });
  }
}

async function getAlerts(req, res) {
  const limit = Math.min(parseInt(req.query.limit) || 50, 200);
  try {
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
  } catch (err) {
    res.status(500).json({ error: "Error al obtener alertas" });
  }
}

async function sendEmailNotification(alert) {
  const { SENDGRID_API_KEY, FROM_EMAIL, PARENT_EMAIL } = process.env;
  if (!SENDGRID_API_KEY || !FROM_EMAIL || !PARENT_EMAIL) return;

  const msg = {
    to: PARENT_EMAIL,
    from: FROM_EMAIL,
    subject: `🚨 ALERTA CRÍTICA: ${alert.category.replace('_', ' ').toUpperCase()}`,
    html: `
      <div style="font-family: sans-serif; border: 2px solid #ef4444; padding: 20px; border-radius: 12px; max-width: 600px;">
        <h2 style="color: #ef4444;">Riesgo Crítico Detectado</h2>
        <p>Dispositivo: <strong>${alert.device_label}</strong></p>
        <p>Mensaje: <em>"${alert.fragment}"</em></p>
        <a href="https://familia-monitoreo.onrender.com" style="background:#4f46e5;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;">Ver Panel</a>
      </div>
    `
  };

  try {
    await sgMail.send(msg);
  } catch (error) {
    console.error("[SENDGRID] Error:", error.message);
  }
}

module.exports = { createAlert, getAlerts };
