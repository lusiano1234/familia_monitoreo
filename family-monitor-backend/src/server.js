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

app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors());
app.use(morgan("dev"));
app.use(express.json());
app.use(express.static(path.join(__dirname, "..", "public")));

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100
});
app.use("/api/auth/login", limiter);

// Rutas
app.post("/api/auth/login", authController.login);
app.post("/api/alerts", requireDeviceAuth, (req, res) => alertController.createAlert(req, res, io));
app.get("/api/alerts", requireAdminAuth, alertController.getAlerts);
app.delete("/api/alerts", requireAdminAuth, alertController.deleteAllAlerts);
app.get("/api/devices", requireAdminAuth, deviceController.getDevices);
app.post("/api/devices", requireAdminAuth, deviceController.createDevice);
app.get("/health", (req, res) => res.json({ ok: true }));

io.on("connection", (socket) => {
  console.log("Panel conectado:", socket.id);
});

const PORT = process.env.PORT || 3000;

initDb()
  .then(() => {
    httpServer.listen(PORT, () => console.log(`Servidor PRO en puerto ${PORT}`));
  })
  .catch((err) => {
    console.error("Error inicializando:", err);
    process.exit(1);
  });
