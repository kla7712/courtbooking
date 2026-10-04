CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('MEMBER', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE courts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(50) NOT NULL UNIQUE,
    surface      VARCHAR(20) NOT NULL CHECK (surface IN ('CLAY', 'HARD', 'ARTIFICIAL_GRASS')),
    indoor       BOOLEAN     NOT NULL DEFAULT FALSE,
    has_lighting BOOLEAN     NOT NULL DEFAULT FALSE,
    active       BOOLEAN     NOT NULL DEFAULT TRUE
);

-- Datos de ejemplo para desarrollo
INSERT INTO courts (name, surface, indoor, has_lighting) VALUES
    ('Pista 1', 'CLAY', FALSE, TRUE),
    ('Pista 2', 'CLAY', FALSE, FALSE),
    ('Pista 3', 'HARD', FALSE, TRUE),
    ('Pista 4', 'HARD', TRUE,  TRUE);
