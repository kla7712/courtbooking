-- Necesaria para combinar "=" (court_id) y "&&" (rangos) en la misma restricción de exclusión
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    court_id   BIGINT      NOT NULL REFERENCES courts (id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time   TIMESTAMPTZ NOT NULL,
    status     VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT bookings_time_order CHECK (end_time > start_time),

    -- Garantía real contra dobles reservas: la base de datos rechaza dos reservas
    -- confirmadas de la misma pista cuyos horarios se solapen, aunque lleguen a la vez.
    -- El rango es [inicio, fin), así que una reserva puede empezar justo cuando acaba otra.
    CONSTRAINT bookings_no_overlap EXCLUDE USING gist (
        court_id WITH =,
        tstzrange(start_time, end_time) WITH &&
    ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX idx_bookings_user ON bookings (user_id, start_time);
