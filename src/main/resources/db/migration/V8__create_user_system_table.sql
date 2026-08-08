CREATE TABLE user_system (
    id         BIGINT      PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    system_id  BIGINT      NOT NULL,
    tenant_id  BIGINT      NOT NULL,   -- redundante DE PROPÓSITO (ver 4.4)
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT uq_user_system UNIQUE (user_id, system_id),
    CONSTRAINT fk_us_user_tenant FOREIGN KEY (user_id, tenant_id)
        REFERENCES "user"(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_us_system_tenant FOREIGN KEY (system_id, tenant_id)
        REFERENCES system_tenant(system_id, tenant_id) ON DELETE CASCADE
);
CREATE INDEX idx_us_user_id   ON user_system(user_id);
CREATE INDEX idx_us_system_id ON user_system(system_id);
CREATE INDEX idx_us_status    ON user_system(status);
