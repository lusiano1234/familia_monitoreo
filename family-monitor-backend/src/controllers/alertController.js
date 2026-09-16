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
  console.log("[EMAIL] Iniciando proceso de envío...");

  const { SMTP_USER, SMTP_PASS, PARENT_EMAIL } = process.env;

  if (!SMTP_USER || !SMTP_PASS || !PARENT_EMAIL) {
    console.log("[EMAIL] ERROR: Variables de entorno incompletas en Render.");
    return;
  }

  // Configuración explícita para Gmail
  const transporter = nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true, // SSL
    auth: {
      user: SMTP_USER,
      pass: SMTP_PASS
    }
  });

  try {
    // Verificar conexión antes de enviar
    console.log("[EMAIL] Verificando conexión con servidor SMTP...");
    await transporter.verify();
    console.log("[EMAIL] Conexión SMTP verificada. Enviando...");

    const mailOptions = {
      from: `"Protección Familiar" <${SMTP_USER}>`,
      to: PARENT_EMAIL,
      subject: `🚨 ALERTA CRÍTICA: ${alert.category.replace('_', ' ').toUpperCase()}`,
      html: `
        <div style="font-family: sans-serif; border: 2px solid #ef4444; padding: 20px; border-radius: 10px;">
          <h2 style="color: #ef4444;">Detección de Riesgo Crítico</h2>
          <p>Se ha detectado una situación de peligro en el dispositivo: <strong>${alert.device_label}</strong></p>
          <hr>
          <p><strong>Categoría:</strong> ${alert.category}</p>
          <p><strong>Aplicación:</strong> ${alert.source_app}</p>
          <div style="background: #f3f4f6; padding: 15px; border-radius: 5px; font-style: italic;">
            "${alert.fragment}"
          </div>
          <p style="margin-top: 20px;">
            <a href="https://familia-monitoreo.onrender.com" style="background: #4f46e5; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Ver Panel de Control</a>
          </p>
        </div>
      `
    };

    const info = await transporter.sendMail(mailOptions);
    console.log("[EMAIL] ÉXITO: Correo enviado correctamente. ID:", info.messageId);
  } catch (err) {
    console.error("[EMAIL] ERROR DETALLADO:", err.message);
    if (err.message.includes("Invalid login")) {
      console.error("[EMAIL] Sugerencia: Revisa que la Contraseña de Aplicación sea correcta y no tenga espacios.");
    }
  }
}

module.exports = { createAlert, getAlerts };
