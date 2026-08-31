package com.mssousa.authserver.domain.model.platform;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformAdminTest {

    private PlatformAdmin.Builder validBuilder() {
        return PlatformAdmin.builder()
                .id(PlatformAdminId.of(1L))
                .username(Username.of("root_admin"))
                .email(Email.of("admin@seudominio.com"))
                .password(Password.fromPlainText("senhaSegura123"))
                .name("Administrador");
    }

    @Test
    void deveCriarPlatformAdminValidoComStatusPadraoActive() {
        PlatformAdmin admin = validBuilder().build();

        assertEquals(PlatformAdminId.of(1L), admin.getId());
        assertEquals(Username.of("root_admin"), admin.getUsername());
        assertEquals(Email.of("admin@seudominio.com"), admin.getEmail());
        assertEquals("Administrador", admin.getName());
        assertTrue(admin.isActive());
        assertTrue(admin.canLogin());
    }

    @Test
    void deveLancarExcecaoQuandoIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> PlatformAdmin.builder()
                        .username(Username.of("root_admin"))
                        .email(Email.of("admin@seudominio.com"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .name("Administrador")
                        .build());
        assertEquals(PlatformAdmin.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoUsernameNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> PlatformAdmin.builder()
                        .id(PlatformAdminId.of(1L))
                        .email(Email.of("admin@seudominio.com"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .name("Administrador")
                        .build());
        assertEquals(Username.DEFAULT_ERROR_USERNAME, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEmailNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> PlatformAdmin.builder()
                        .id(PlatformAdminId.of(1L))
                        .username(Username.of("root_admin"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .name("Administrador")
                        .build());
        assertEquals(Email.DEFAULT_ERROR_EMAIL, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoPasswordNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> PlatformAdmin.builder()
                        .id(PlatformAdminId.of(1L))
                        .username(Username.of("root_admin"))
                        .email(Email.of("admin@seudominio.com"))
                        .name("Administrador")
                        .build());
        assertEquals(Password.DEFAULT_ERROR_PASSWORD, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoNomeNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> PlatformAdmin.builder()
                        .id(PlatformAdminId.of(1L))
                        .username(Username.of("root_admin"))
                        .email(Email.of("admin@seudominio.com"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .build());
        assertEquals(PlatformAdmin.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void deveDesativarEImpedirLogin() {
        PlatformAdmin admin = validBuilder().build();
        admin.deactivate();
        assertFalse(admin.isActive());
        assertFalse(admin.canLogin());
    }

    @Test
    void ativacaoDeveSerIdempotente() {
        PlatformAdmin admin = validBuilder().status(PlatformAdminStatus.INACTIVE).build();
        admin.activate();
        admin.activate();
        assertTrue(admin.isActive());
    }

    @Test
    void deveTrocarSenha() {
        PlatformAdmin admin = validBuilder().build();
        admin.changePassword(Password.fromPlainText("outraSenhaSegura"));
        assertTrue(admin.verifyPassword("outraSenhaSegura"));
        assertFalse(admin.verifyPassword("senhaSegura123"));
    }

    @Test
    void mustChangePasswordDeveSerFalsoPorPadrao() {
        PlatformAdmin admin = validBuilder().build();
        assertFalse(admin.mustChangePassword());
    }

    @Test
    void builderDeveAceitarMustChangePasswordTrue() {
        PlatformAdmin admin = validBuilder().mustChangePassword(true).build();
        assertTrue(admin.mustChangePassword());
    }

    @Test
    void trocarSenhaDeveLimparMustChangePassword() {
        PlatformAdmin admin = validBuilder().mustChangePassword(true).build();
        assertTrue(admin.mustChangePassword());

        admin.changePassword(Password.fromPlainText("outraSenhaSegura"));

        assertFalse(admin.mustChangePassword());
    }

    @Test
    void deveLancarExcecaoAoTrocarSenhaParaNula() {
        PlatformAdmin admin = validBuilder().build();
        DomainException exception = assertThrows(DomainException.class, () -> admin.changePassword(null));
        assertEquals(Password.DEFAULT_ERROR_PASSWORD, exception.getMessage());
    }

    @Test
    void deveAtualizarNome() {
        PlatformAdmin admin = validBuilder().build();
        admin.updateName("Outro Nome");
        assertEquals("Outro Nome", admin.getName());
    }

    @Test
    void deveLancarExcecaoAoAtualizarNomeParaVazio() {
        PlatformAdmin admin = validBuilder().build();
        DomainException exception = assertThrows(DomainException.class, () -> admin.updateName(""));
        assertEquals(PlatformAdmin.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void deveAtualizarEmail() {
        PlatformAdmin admin = validBuilder().build();
        admin.updateEmail(Email.of("novo@seudominio.com"));
        assertEquals(Email.of("novo@seudominio.com"), admin.getEmail());
    }

    @Test
    void deveLancarExcecaoAoAtualizarEmailParaNulo() {
        PlatformAdmin admin = validBuilder().build();
        DomainException exception = assertThrows(DomainException.class, () -> admin.updateEmail(null));
        assertEquals(Email.DEFAULT_ERROR_EMAIL, exception.getMessage());
    }
}
