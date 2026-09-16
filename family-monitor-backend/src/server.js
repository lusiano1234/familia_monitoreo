require("dotenv").config();
const express = require("express");
const cors = require("cors");
const path = require("path");
const helmet = require("helmet");
const morgan = require("morgan");
const { createServer } = require("http");
const { Server } = require("socket.io");
const rateLimit = require("express-rate-limit");

const { initDb } = require("./db");
const { requireDeviceAuth, requireAdminAuth } = require("./middlewares/auth");
const alertController = require("./controllers/alertController");
const deviceController = require("./controllers/deviceController");
const authController = require("./controllers/authController");

const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: { origin: "*" }
});

// --- Middleware de Seguridad y Logs ---
app.use(helmet({ contentSecurityPolicy: false })); // Permitir scripts inline para el panel simple
app.use(cors());
app.use(morgan("dev"));
app.use(express.json());
app.use(express.static(path.join(__dirname, "..", "public")));

// --- Limitador de peticiones ---
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutos
  max: 100 // Máximo 100 peticiones por IP
});
app.use("/api/auth/login", limiter);

// --- Rutas ---

// 1. Autenticación
app.post("/api/auth/login", authController.login);

// 2. Alertas (Usadas por la app Android)
app.post("/api/alerts", requireDeviceAuth, (req, res) => alertController.createAlert(req, res, io));

// 3. Panel Administrativo (Padres)
app.get("/api/alerts", requireAdminAuth, alertController.getAlerts);
app.get("/api/devices", requireAdminAuth, deviceController.getDevices);
app.post("/api/devices", requireAdminAuth, deviceController.createDevice);

// 4. Health Check
app.get("/health", (req, res) => res.json({ ok: true, timestamp: new Date() }));

// --- WebSockets ---
io.on("connection", (socket) => {
  console.log("Panel web conectado (Socket ID):", socket.id);
  socket.on("disconnect", () => console.log("Panel web desconectado"));
});

// --- Inicialización ---
const PORT = process.env.PORT || 3000;

initDb()
  .then(() => {
    httpServer.listen(PORT, () => {
      console.log(`=========================================`);
      console.log(`   BACKEND PROFESIONAL INICIADO`);
      console.log(`   Puerto: ${PORT}`);
      console.log(`   Ambiente: ${process.env.NODE_ENV || "development"}`);
      console.log(`=========================================`);
    });
  })
  .catch((err) => {
    console.error("Fallo crítico en la inicialización:", err);
    process.exit(1);
  });
