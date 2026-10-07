-- Tarifas por superficie, tipo de día y franja horaria
CREATE TABLE price_rules (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    surface        VARCHAR(20)   NOT NULL CHECK (surface IN ('CLAY', 'HARD', 'ARTIFICIAL_GRASS')),
    day_type       VARCHAR(10)   NOT NULL CHECK (day_type IN ('WEEKDAY', 'WEEKEND')),
    start_time     TIME          NOT NULL,
    end_time       TIME          NOT NULL,
    price_per_hour NUMERIC(6, 2) NOT NULL CHECK (price_per_hour >= 0),

    CONSTRAINT price_rules_time_order CHECK (end_time > start_time)
);

INSERT INTO price_rules (surface, day_type, start_time, end_time, price_per_hour) VALUES
    -- Tierra batida
    ('CLAY', 'WEEKDAY', '08:00', '17:00', 12.00),   -- valle
    ('CLAY', 'WEEKDAY', '17:00', '22:00', 16.00),   -- punta
    ('CLAY', 'WEEKEND', '08:00', '22:00', 16.00),
    -- Pista dura
    ('HARD', 'WEEKDAY', '08:00', '17:00', 10.00),
    ('HARD', 'WEEKDAY', '17:00', '22:00', 14.00),
    ('HARD', 'WEEKEND', '08:00', '22:00', 14.00),
    -- Césped artificial
    ('ARTIFICIAL_GRASS', 'WEEKDAY', '08:00', '17:00', 9.00),
    ('ARTIFICIAL_GRASS', 'WEEKDAY', '17:00', '22:00', 13.00),
    ('ARTIFICIAL_GRASS', 'WEEKEND', '08:00', '22:00', 13.00);

-- Bloqueos de pista: mantenimiento, lluvia, torneos, clases...
CREATE TABLE court_blocks (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    court_id   BIGINT       NOT NULL REFERENCES courts (id),
    start_time TIMESTAMPTZ  NOT NULL,
    end_time   TIMESTAMPTZ  NOT NULL,
    reason     VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT court_blocks_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_court_blocks_court ON court_blocks (court_id, start_time);

-- Precio final de cada reserva. Se guarda en la reserva para que un cambio
-- posterior de tarifas no altere lo que ya se cobró.
-- Las reservas que ya existían se quedan con precio 0.
ALTER TABLE bookings ADD COLUMN price NUMERIC(8, 2) NOT NULL DEFAULT 0;
ALTER TABLE bookings ALTER COLUMN price DROP DEFAULT;
