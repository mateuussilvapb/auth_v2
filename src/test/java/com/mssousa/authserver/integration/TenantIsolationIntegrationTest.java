package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes obrigatórios de isolamento entre tenants (seção 8.3 do plano) que são
 * verificáveis na camada de persistência.
 */
class TenantIsolationIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private SystemTenantRepository systemTenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void uniqueTenantIdEmailPermiteMesmoEmailEmTenantsDistintos() {
        Tenant acme = createAndSaveTenant("acme");
        Tenant globex = createAndSaveTenant("globex");

        User userAcme = createAndSaveUser(acme.getId(), "joao_acme", "joao@empresa.com");
        User userGlobex = createAndSaveUser(globex.getId(), "joao_globex", "joao@empresa.com");

        assertNotEquals(userAcme.getId(), userGlobex.getId());
        assertEquals(userAcme.getEmail(), userGlobex.getEmail());
        assertTrue(userRepository.existsByTenantIdAndEmail(acme.getId(), userAcme.getEmail()));
        assertTrue(userRepository.existsByTenantIdAndEmail(globex.getId(), userGlobex.getEmail()));
    }

    @Test
    void criarUserSystemCruzandoTenantsFalhaNaFkDoBancoViaSqlDireto() {
        Tenant tenantDoSistema = createAndSaveTenant("acme");
        Tenant outroTenant = createAndSaveTenant("globex");
        System system = createAndSaveSystem("CRM_ACME");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenantDoSistema.getId())
                .systemId(system.getId())
                .build());
        User userDeOutroTenant = createAndSaveUser(outroTenant.getId(), "joao_globex", "joao@globex.com");

        long fakeUserSystemId = idGenerator.generate();

        // Tenta inserir um UserSystem ligando um usuário do tenant "globex" a um sistema
        // cujo system_tenant só existe para o tenant "acme" — ambas as FKs compostas
        // (fk_us_user_tenant e fk_us_system_tenant, seção 4.4) devem rejeitar o insert,
        // mesmo via SQL direto, sem passar por nenhuma validação de aplicação.
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO user_system (id, user_id, system_id, tenant_id, status, created_at, created_by) " +
                        "VALUES (?, ?, ?, ?, 'ACTIVE', ?, 'test')",
                fakeUserSystemId,
                userDeOutroTenant.getId().value(),
                system.getId().value(),
                tenantDoSistema.getId().value(),
                LocalDateTime.now()
        ));
    }
}
