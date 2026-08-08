-- PKCE exige múltiplas URIs por client (dev, homolog, prod)
CREATE TABLE system_redirect_uri (
    id         BIGINT       PRIMARY KEY,
    system_id  BIGINT       NOT NULL,
    uri        VARCHAR(500) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT fk_sru_system FOREIGN KEY (system_id)
        REFERENCES system(id) ON DELETE CASCADE,
    CONSTRAINT uq_sru UNIQUE (system_id, uri)
);
