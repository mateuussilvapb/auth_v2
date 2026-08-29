package com.mssousa.authserver.integration;

import com.mssousa.authserver.application.port.out.PasswordResetTokenRepository;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetToken;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetTokenId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.ResetTokenValue;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void deveSalvarEBuscarPorHashDoValor() {
        Tenant tenant = createAndSaveTenant("acme");
        User user = createAndSaveUser(tenant.getId(), "joao_silva", "joao@acme.com");

        Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        PasswordResetToken.GeneratedToken generated = PasswordResetToken.create(
                PasswordResetTokenId.of(idGenerator.generate()), user.getId(), expiresAt);

        passwordResetTokenRepository.save(generated.token());

        ResetTokenValue hashDoValorRecebido = ResetTokenValue.ofRawToken(generated.rawValue());
        PasswordResetToken found = passwordResetTokenRepository.findByValue(hashDoValorRecebido).orElseThrow();

        assertEquals(user.getId(), found.getUserId());
        assertFalse(found.isUsed());
    }

    @Test
    void naoDeveEncontrarPorHashIncorreto() {
        Tenant tenant = createAndSaveTenant("globex");
        User user = createAndSaveUser(tenant.getId(), "maria_souza", "maria@globex.com");
        PasswordResetToken.GeneratedToken generated = PasswordResetToken.create(
                PasswordResetTokenId.of(idGenerator.generate()), user.getId(),
                Instant.now().plus(30, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(generated.token());

        ResetTokenValue outroHash = ResetTokenValue.ofRawToken("x".repeat(40));
        assertTrue(passwordResetTokenRepository.findByValue(outroHash).isEmpty());
    }

    @Test
    void deveMarcarComoUtilizadoEPersistir() {
        Tenant tenant = createAndSaveTenant("initech");
        User user = createAndSaveUser(tenant.getId(), "peter_gibbons", "peter@initech.com");
        PasswordResetToken.GeneratedToken generated = PasswordResetToken.create(
                PasswordResetTokenId.of(idGenerator.generate()), user.getId(),
                Instant.now().plus(30, ChronoUnit.MINUTES));
        PasswordResetToken saved = passwordResetTokenRepository.save(generated.token());

        saved.markAsUsed();
        passwordResetTokenRepository.save(saved);

        PasswordResetToken found = passwordResetTokenRepository.findById(saved.getId()).orElseThrow();
        assertTrue(found.isUsed());
    }
}
