CREATE TABLE password_reset_token (
    id         BIGINT       PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,   -- SHA-256 do token, nunca o valor puro
    user_id    BIGINT       NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT uq_prt_token UNIQUE (token_hash),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id)
        REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE INDEX idx_prt_expires_at ON password_reset_token(expires_at);
