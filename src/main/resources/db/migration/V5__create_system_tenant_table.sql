CREATE TABLE system_tenant (
    id         BIGINT      PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL,
    system_id  BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT fk_st_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_st_system FOREIGN KEY (system_id)
        REFERENCES system(id) ON DELETE CASCADE,
    -- 1 sistema : 1 tenant (decisão D3)
    CONSTRAINT uq_st_system UNIQUE (system_id)
);
CREATE INDEX idx_st_tenant_id ON system_tenant(tenant_id);
-- Necessário para a FK composta de user_system (ver 4.4)
CREATE UNIQUE INDEX uq_st_system_tenant ON system_tenant(system_id, tenant_id);
