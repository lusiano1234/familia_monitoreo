const { pool } = require("../db");
const crypto = require("crypto");

async function createDevice(req, res) {
  const { label } = req.body || {};
  const token = crypto.randomBytes(24).toString("hex");
  try {
    await pool.query(
      "INSERT INTO devices (device_token, label) VALUES ($1, $2)",
      [token, label || null]
    );
    res.status(201).json({ deviceToken: token, label: label || null });
  } catch (err) {
    res.status(500).json({ error: "Error creando dispositivo" });
  }
}

async function getDevices(req, res) {
  try {
    const result = await pool.query(
      "SELECT id, device_token, label, created_at FROM devices ORDER BY created_at DESC"
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: "Error listando dispositivos" });
  }
}

module.exports = { createDevice, getDevices };
