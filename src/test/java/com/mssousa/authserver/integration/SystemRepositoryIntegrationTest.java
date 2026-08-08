package com.mssousa.authserver.integration;

import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Test
    void deveSalvarERecuperarComRedirectUris() {
        System saved = createAndSaveSystem("CRM_ACME");

        System found = systemRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1, found.getRedirectUris().size());
        assertTrue(found.matchesRedirectUri("https://crm_acme.example.com/callback"));
    }

    @Test
    void deveBuscarPorClientId() {
        createAndSaveSystem("BACKOFFICE_ACME");

        System found = systemRepository.findByClientId(ClientId.of("BACKOFFICE_ACME")).orElseThrow();
        assertEquals("BACKOFFICE_ACME", found.getClientId().value());
    }

    @Test
    void deveAdicionarRedirectUriEPersistirNaAtualizacao() {
        System saved = createAndSaveSystem("CRM_GLOBEX");
        saved.addRedirectUri(RedirectUri.of("https://crm_globex.example.com/dev-callback"));

        systemRepository.save(saved);

        System found = systemRepository.findById(saved.getId()).orElseThrow();
        assertEquals(2, found.getRedirectUris().size());
    }

    @Test
    void existsByClientIdDeveRefletirEstadoReal() {
        assertFalse(systemRepository.existsByClientId(ClientId.of("INEXISTENTE")));
        createAndSaveSystem("INITECH_ERP");
        assertTrue(systemRepository.existsByClientId(ClientId.of("INITECH_ERP")));
    }
}
