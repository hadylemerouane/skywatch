CREATE TABLE IF NOT EXISTS positions (
  icao24            VARCHAR(6) PRIMARY KEY,
  callsign          VARCHAR(10),
  latitude          DOUBLE PRECISION NOT NULL,
  longitude         DOUBLE PRECISION NOT NULL,
  altitude_ft       DOUBLE PRECISION NOT NULL,
  ground_speed_kt   DOUBLE PRECISION NOT NULL,
  track_deg         DOUBLE PRECISION NOT NULL,
  vertical_rate_fpm DOUBLE PRECISION NOT NULL,
  updated_at        TIMESTAMPTZ NOT NULL
);
