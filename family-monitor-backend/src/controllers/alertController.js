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
  console.log("[EMAIL] Iniciando proceso de envío (Puerto 465 - SSL Seguro)...");

  const { SMTP_USER, SMTP_PASS, PARENT_EMAIL } = process.env;

  if (!SMTP_USER || !SMTP_PASS || !PARENT_EMAIL) {
    console.log("[EMAIL] ERROR: Variables de entorno incompletas.");
    return;
  }

  // Configuración directa y robusta para Gmail en la nube
  const transporter = nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true, // Forzamos SSL directo
    auth: {
      user: SMTP_USER,
      pass: SMTP_PASS
    },
    tls: {
      // No fallar por certificados auto-firmados o problemas de red local
      rejectUnauthorized: false
    },
    debug: true,
    logger: true
  });

  try {
    console.log("[EMAIL] Enviando mensaje a:", PARENT_EMAIL);

    const mailOptions = {
      from: `"Protección Familiar" <${SMTP_USER}>`,
      to: PARENT_EMAIL,
      subject: `🚨 ALERTA CRÍTICA: ${alert.category.replace('_', ' ').toUpperCase()}`,
      html: `
        <div style="font-family: sans-serif; border: 2px solid #ef4444; padding: 20px; border-radius: 10px; max-width: 600px; background-color: #ffffff;">
          <h2 style="color: #ef4444; margin-top: 0;">Detección de Riesgo Crítico</h2>
          <p style="font-size: 16px; color: #111827;">Se ha detectado una situación de peligro en el dispositivo de monitoreo.</p>
          <div style="background: #f3f4f6; padding: 15px; border-radius: 8px; margin: 20px 0;">
            <p><strong>Categoría:</strong> ${alert.category}</p>
            <p><strong>Aplicación:</strong> ${alert.source_app}</p>
            <p style="font-style: italic; color: #374151; font-size: 18px; border-left: 4px solid #ef4444; padding-left: 10px;">
              "${alert.fragment}"
            </p>
          </div>
          <p style="color: #6b7280; font-size: 12px;">Detectado el: ${new Date(alert.received_at).toLocaleString()}</p>
          <div style="margin-top: 25px; text-align: center;">
            <a href="https://familia-monitoreo.onrender.com" style="background: #4f46e5; color: white; padding: 12px 25px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">
              IR AL PANEL DE CONTROL
            </a>
          </div>
        </div>
      `
    };

    const info = await transporter.sendMail(mailOptions);
    console.log("[EMAIL] ÉXITO TOTAL: Correo enviado. ID:", info.messageId);
  } catch (err) {
    console.error("[EMAIL] FALLO EN EL ENVÍO:", err.message);
    console.error("[EMAIL] DETALLE TÉCNICO:", err.code || "Sin código");
  }
}

module.exports = { createAlert, getAlerts };
