package com.mssousa.authserver.integration;

import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Test
    void deveSalvarEBuscarPorTenantEUsername() {
        Tenant tenant = createAndSaveTenant("acme");
        createAndSaveUser(tenant.getId(), "joao_silva", "joao@acme.com");

        User found = userRepository.findByTenantIdAndUsername(tenant.getId(), Username.of("joao_silva")).orElseThrow();
        assertEquals("joao@acme.com", found.getEmail().value());
    }

    @Test
    void deveBuscarPorTenantEEmail() {
        Tenant tenant = createAndSaveTenant("globex");
        createAndSaveUser(tenant.getId(), "maria_souza", "maria@globex.com");

        User found = userRepository.findByTenantIdAndEmail(tenant.getId(), Email.of("maria@globex.com")).orElseThrow();
        assertEquals("maria_souza", found.getUsername().value());
    }

    @Test
    void naoDeveEncontrarUsuarioDeOutroTenant() {
        Tenant tenantA = createAndSaveTenant("acme");
        Tenant tenantB = createAndSaveTenant("globex");
        createAndSaveUser(tenantA.getId(), "joao_silva", "joao@acme.com");

        assertTrue(userRepository.findByTenantIdAndUsername(tenantB.getId(), Username.of("joao_silva")).isEmpty());
    }

    @Test
    void existsByTenantIdAndUsernameDeveRefletirEstadoReal() {
        Tenant tenant = createAndSaveTenant("initech");
        assertFalse(userRepository.existsByTenantIdAndUsername(tenant.getId(), Username.of("peter_gibbons")));

        createAndSaveUser(tenant.getId(), "peter_gibbons", "peter@initech.com");
        assertTrue(userRepository.existsByTenantIdAndUsername(tenant.getId(), Username.of("peter_gibbons")));
    }
}
