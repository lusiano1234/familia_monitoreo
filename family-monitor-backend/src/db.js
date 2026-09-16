const { Pool } = require("pg");

// Render inyecta DATABASE_URL automáticamente si conectás una base
// Postgres al servicio. En local, usá un .env con DATABASE_URL propio.
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.DATABASE_URL?.includes("localhost")
    ? false
    : { rejectUnauthorized: false },
});

async function initDb() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS devices (
      id SERIAL PRIMARY KEY,
      device_token TEXT UNIQUE NOT NULL,
      label TEXT,
      created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS alerts (
      id SERIAL PRIMARY KEY,
      device_token TEXT NOT NULL,
      source_app TEXT NOT NULL,
      category TEXT NOT NULL,
      level TEXT NOT NULL,
      fragment TEXT NOT NULL,
      device_timestamp BIGINT NOT NULL,
      battery_level INTEGER,
      connection_type TEXT,
      received_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );
  `);

  await pool.query(`
    CREATE INDEX IF NOT EXISTS idx_alerts_received_at ON alerts (received_at DESC);
  `);

  // --- MIGRACIONES: Asegurar que las columnas nuevas existan en tablas viejas ---
  await pool.query(`
    ALTER TABLE alerts ADD COLUMN IF NOT EXISTS battery_level INTEGER;
    ALTER TABLE alerts ADD COLUMN IF NOT EXISTS connection_type TEXT;
  `);
}

module.exports = { pool, initDb };
