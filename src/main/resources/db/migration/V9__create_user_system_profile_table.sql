CREATE TABLE user_system_profile (
    id                BIGINT      PRIMARY KEY,
    user_system_id    BIGINT      NOT NULL,
    system_profile_id BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(50),
    CONSTRAINT fk_usp_user_system FOREIGN KEY (user_system_id)
        REFERENCES user_system(id) ON DELETE CASCADE,
    CONSTRAINT fk_usp_profile FOREIGN KEY (system_profile_id)
        REFERENCES system_profile(id) ON DELETE CASCADE,
    CONSTRAINT uq_usp UNIQUE (user_system_id, system_profile_id)
);
CREATE INDEX idx_usp_user_system_id ON user_system_profile(user_system_id);
CREATE INDEX idx_usp_profile_id     ON user_system_profile(system_profile_id);
CREATE INDEX idx_usp_status         ON user_system_profile(status);
