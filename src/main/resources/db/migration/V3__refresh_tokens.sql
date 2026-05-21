CREATE TABLE refresh_tokens (
             id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
             token      VARCHAR(512) NOT NULL,
             username   VARCHAR(100) NOT NULL,
             expires_at DATETIME     NOT NULL,
             revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
             created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_username ON refresh_tokens(username);