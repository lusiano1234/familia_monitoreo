const jwt = require("jsonwebtoken");
const { JWT_SECRET } = require("../middlewares/auth");

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || "admin123";

/**
 * Login administrativo para los padres
 */
async function login(req, res) {
  const { password } = req.body || {};

  if (!password) {
    return res.status(400).json({ error: "Contraseña requerida" });
  }

  // En un sistema real usaríamos bcrypt para comparar hashes,
  // pero mantendremos la compatibilidad con tu ADMIN_PASSWORD actual.
  if (password !== ADMIN_PASSWORD) {
    return res.status(401).json({ error: "Contraseña incorrecta" });
  }

  // Generar token JWT válido por 24 horas
  const token = jwt.sign({ role: "admin" }, JWT_SECRET, { expiresIn: "24h" });

  res.json({ token });
}

module.exports = { login };
