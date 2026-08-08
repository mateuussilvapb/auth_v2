CREATE TABLE "user" (
    id            BIGINT       PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    VARCHAR(50),
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    -- Identidade escopada ao tenant (decisão D2)
    CONSTRAINT uq_user_username UNIQUE (tenant_id, username),
    CONSTRAINT uq_user_email    UNIQUE (tenant_id, email)
);
CREATE INDEX idx_user_tenant_id ON "user"(tenant_id);
CREATE INDEX idx_user_status    ON "user"(status);
-- Necessário para a FK composta de user_system (ver 4.4)
CREATE UNIQUE INDEX uq_user_id_tenant ON "user"(id, tenant_id);
