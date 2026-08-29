CREATE TABLE platform_admin (
    id            BIGINT       PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    VARCHAR(50),
    CONSTRAINT uq_platform_admin_username UNIQUE (username),
    CONSTRAINT uq_platform_admin_email    UNIQUE (email)
);
