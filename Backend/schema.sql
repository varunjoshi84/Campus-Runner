-- Run: psql -d campus_runner -f schema.sql
-- gen_random_uuid() Postgres 13+ mein built-in hai.

CREATE TABLE IF NOT EXISTS users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  google_id     TEXT UNIQUE,
  password_hash TEXT,
  name          TEXT,
  email         TEXT UNIQUE,
  phone         TEXT,
  created_at    TIMESTAMP DEFAULT now()
);

-- "I'm at the store" = ek run
CREATE TABLE IF NOT EXISTS runs (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  runner_id  UUID NOT NULL REFERENCES users(id),
  store_name TEXT NOT NULL,
  lat        DOUBLE PRECISION NOT NULL,
  lng        DOUBLE PRECISION NOT NULL,
  active     BOOLEAN DEFAULT true,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT now()
);

-- Doosre students ki item requests
CREATE TABLE IF NOT EXISTS requests (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  run_id       UUID NOT NULL REFERENCES runs(id),
  requester_id UUID NOT NULL REFERENCES users(id),
  item         TEXT NOT NULL,
  tip          INT  NOT NULL CHECK (tip >= 0),
  status       TEXT NOT NULL DEFAULT 'OPEN'
               CHECK (status IN ('OPEN','ACCEPTED','BOUGHT','DELIVERED','CANCELLED')),
  created_at   TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_runs_active ON runs (active, expires_at);
CREATE INDEX IF NOT EXISTS idx_requests_run ON requests (run_id);
CREATE INDEX IF NOT EXISTS idx_requests_requester ON requests (requester_id);
