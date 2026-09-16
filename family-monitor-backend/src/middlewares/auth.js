const jwt = require("jsonwebtoken");
const { pool } = require("../db");

const JWT_SECRET = process.env.JWT_SECRET || "super-secret-key-123";

/**
 * Autentica dispositivos Android mediante el Device Token (Bearer token)
 */
async function requireDeviceAuth(req, res, next) {
  const auth = req.headers.authorization || "";
  const token = auth.startsWith("Bearer ") ? auth.slice(7) : null;

  console.log(`[AUTH] Intento de acceso de dispositivo. Token: ${token ? (token.substring(0, 8) + "...") : "NINGUNO"}`);

  if (!token) {
    return res.status(401).json({ error: "Falta token de dispositivo" });
  }

  try {
    const result = await pool.query(
      "SELECT id, label FROM devices WHERE device_token = $1",
      [token]
    );

    if (result.rowCount === 0) {
      return res.status(401).json({ error: "Token de dispositivo inválido" });
    }

    req.device = result.rows[0];
    req.deviceToken = token;
    next();
  } catch (err) {
    console.error("Error en device auth:", err);
    res.status(500).json({ error: "Error de servidor" });
  }
}

/**
 * Autentica a los padres en el panel web mediante JWT
 */
function requireAdminAuth(req, res, next) {
  const auth = req.headers.authorization || "";
  const token = auth.startsWith("Bearer ") ? auth.slice(7) : null;

  if (!token) {
    return res.status(401).json({ error: "No autorizado. Inicie sesión." });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.admin = decoded;
    next();
  } catch (err) {
    res.status(403).json({ error: "Sesión expirada o inválida" });
  }
}

module.exports = { requireDeviceAuth, requireAdminAuth, JWT_SECRET };
