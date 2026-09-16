const { pool } = require("../db");
const nodemailer = require("nodemailer");

/**
 * Recibe alerta de la app Android
 */
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

    // 1. Emitir por WebSockets en tiempo real al panel
    if (io) {
      io.emit("new_alert", newAlert);
    }

    // 2. Notificación proactiva
    if (level === "HIGH") {
      console.log(`[ALERT] CRITICAL: ${category} detectado en ${sourceApp}`);
      sendEmailNotification(newAlert);
    }

    res.status(201).json({ ok: true });
  } catch (err) {
    console.error("Error guardando alerta:", err);
    res.status(500).json({ error: "Error interno" });
  }
}

/**
 * Lista alertas para el panel web
 */
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

/**
 * Skeleton para notificaciones por email
 */
async function sendEmailNotification(alert) {
  // Configura esto en Render con tus variables de entorno
  // SMTP_USER, SMTP_PASS, PARENT_EMAIL
  if (!process.env.SMTP_USER) return;

  const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS }
  });

  const mailOptions = {
    from: `"Monitor Familiar" <${process.env.SMTP_USER}>`,
    to: process.env.PARENT_EMAIL,
    subject: `⚠️ ALERTA CRÍTICA: ${alert.category}`,
    text: `Se ha detectado un riesgo nivel ${alert.level} en el dispositivo ${alert.device_label}.\n\nApp: ${alert.source_app}\nFragmento: "${alert.fragment}"`
  };

  try {
    await transporter.sendMail(mailOptions);
    console.log("Email de alerta enviado.");
  } catch (err) {
    console.error("Error enviando email:", err);
  }
}

module.exports = { createAlert, getAlerts };
