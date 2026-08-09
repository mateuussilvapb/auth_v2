package com.mssousa.authserver.integration;

import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

class SystemRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    /**
     * Regressão: {@code redirectUris} é {@code @OneToMany} lazy — sem
     * {@code @EntityGraph} em {@code SystemJpaRepository}, essa leitura lançava
     * {@code LazyInitializationException} fora de uma transação (o cenário real de
     * {@code SystemRegisteredClientRepository}, chamado direto pelo filter chain do Spring
     * Security, sem `@Transactional` de serviço de aplicação por cima). Descoberto só ao
     * rodar a aplicação de verdade contra um Postgres real — os demais testes desta classe
     * nunca pegam isso porque {@code AbstractRepositoryIntegrationTest} é `@Transactional`,
     * mantendo a sessão Hibernate aberta durante todo o teste.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveCarregarRedirectUrisForaDeUmaTransacao() {
        // Fora de uma transação de teste, a linha não é revertida sozinha no final —
        // precisa limpar manualmente para não vazar para os outros testes da suíte
        // (Testcontainers reusa o mesmo Postgres entre classes, seção de Notas do
        // PROGRESS.md).
        System saved = createAndSaveSystem("CRM_SEM_TRANSACAO");
        try {
            System found = systemRepository.findByClientId(ClientId.of("CRM_SEM_TRANSACAO")).orElseThrow();
            assertEquals(1, found.getRedirectUris().size());
        } finally {
            systemRepository.deleteById(saved.getId());
        }
    }

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
