CREATE TABLE units (
                       id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
                       num_unidad      VARCHAR(100) NOT NULL UNIQUE,
                       horario_fijo    BOOLEAN      NOT NULL DEFAULT FALSE,
                       hora_inicio     TIME         NOT NULL,
                       hora_fin        TIME         NOT NULL,
                       tracking_number VARCHAR(255) NULL,
                       active          BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE users (
                       id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
                       username      VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          ENUM('ADMIN','USER') NOT NULL,
                       active        BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);