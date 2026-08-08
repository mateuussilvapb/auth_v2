CREATE TABLE tenant (
    id          BIGINT       PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(50),
    CONSTRAINT uq_tenant_code UNIQUE (code)
);
CREATE INDEX idx_tenant_status ON tenant(status);
