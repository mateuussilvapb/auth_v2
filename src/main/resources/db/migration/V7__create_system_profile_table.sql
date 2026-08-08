CREATE TABLE system_profile (
    id          BIGINT       PRIMARY KEY,
    system_id   BIGINT       NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(50),
    CONSTRAINT fk_sp_system FOREIGN KEY (system_id)
        REFERENCES system(id) ON DELETE CASCADE,
    -- ADMIN pode existir no sistema A e no B; nunca duas vezes no mesmo
    CONSTRAINT uq_sp_code UNIQUE (system_id, code)
);
CREATE INDEX idx_sp_system_id ON system_profile(system_id);
CREATE INDEX idx_sp_status    ON system_profile(status);
