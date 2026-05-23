ALTER TABLE refresh_tokens
        DROP INDEX idx_refresh_tokens_username,
        DROP COLUMN username;

ALTER TABLE refresh_tokens
        ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0,
        ADD INDEX idx_refresh_tokens_user_id (user_id),
        ADD CONSTRAINT fk_refresh_tokens_users
            FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE refresh_tokens
        ALTER COLUMN user_id DROP DEFAULT;

