package com.mssousa.authserver.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirma que as migrations V1-V11 (seção 4 do plano) aplicam sem erro num Postgres
 * real e criam todas as tabelas esperadas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlywayMigrationIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "tenant", "platform_admin", "system", "system_redirect_uri", "system_tenant",
            "user", "system_profile", "user_system", "user_system_profile",
            "password_reset_token", "oauth2_authorization", "oauth2_authorization_consent"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void todasAsMigrationsDevemCriarAsTabelasEsperadas() {
        List<String> existingTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        for (String expected : EXPECTED_TABLES) {
            assertTrue(existingTables.contains(expected), "Tabela ausente: " + expected);
        }
    }
}
