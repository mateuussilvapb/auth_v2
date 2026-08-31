ALTER TABLE platform_admin
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Seed inicial (seção 10, Fase 10): primeiro platform admin, senha temporária forçando
-- troca no primeiro acesso (POST /admin/api/v1/platform-admins/me/password limpa a flag).
-- Hash BCrypt (custo 12) de "TrocarEssaSenha123" — mesma senha temporária já usada nos
-- reseeds manuais registrados em PROGRESS.md; trocar imediatamente em qualquer ambiente
-- real (must_change_password=TRUE bloqueia todo uso do console até a troca).
INSERT INTO platform_admin (id, username, email, password_hash, name, status, must_change_password, created_at)
VALUES (
    100000000000000001,
    'admin',
    'admin@example.com',
    '$2a$12$EmbfNwevorDa2OuHYU9RMOoi87R2hzP9JF2PIqHhj5QwojGmx8PJm',
    'Administrador',
    'ACTIVE',
    TRUE,
    NOW()
);
