CREATE TABLE system (
    id             BIGINT       PRIMARY KEY,
    client_id      VARCHAR(100) NOT NULL,
    client_secret  VARCHAR(255),               -- NULL para clients públicos (SPA)
    name           VARCHAR(150) NOT NULL,
    public_client  BOOLEAN      NOT NULL DEFAULT TRUE,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP,
    created_by     VARCHAR(50),
    CONSTRAINT uq_system_client_id UNIQUE (client_id)
);
CREATE INDEX idx_system_status ON system(status);
