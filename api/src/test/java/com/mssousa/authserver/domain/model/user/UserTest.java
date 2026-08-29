package com.mssousa.authserver.domain.model.user;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User.Builder validBuilder() {
        return User.builder()
                .id(UserId.of(1L))
                .tenantId(TenantId.of(1L))
                .username(Username.of("joao_silva"))
                .email(Email.of("joao@acme.com"))
                .password(Password.fromPlainText("senhaSegura123"))
                .name("João da Silva");
    }

    @Test
    void deveCriarUsuarioValidoComStatusPadraoActive() {
        User user = validBuilder().build();

        assertEquals(UserId.of(1L), user.getId());
        assertEquals(TenantId.of(1L), user.getTenantId());
        assertEquals(Username.of("joao_silva"), user.getUsername());
        assertEquals(Email.of("joao@acme.com"), user.getEmail());
        assertEquals("João da Silva", user.getName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
    }

    @Test
    void deveLancarExcecaoQuandoIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> User.builder()
                        .tenantId(TenantId.of(1L))
                        .username(Username.of("joao_silva"))
                        .email(Email.of("joao@acme.com"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .name("João")
                        .build());
        assertEquals(User.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoTenantIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> User.builder()
                        .id(UserId.of(1L))
                        .username(Username.of("joao_silva"))
                        .email(Email.of("joao@acme.com"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .name("João")
                        .build());
        assertEquals(User.ERROR_TENANT_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoUsernameNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> User.builder()
                        .id(UserId.of(1L))
                        .tenantId(TenantId.of(1L))
                        .email(Email.of("joao@acme.com"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .name("João")
                        .build());
        assertEquals(Username.DEFAULT_ERROR_USERNAME, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEmailNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> User.builder()
                        .id(UserId.of(1L))
                        .tenantId(TenantId.of(1L))
                        .username(Username.of("joao_silva"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .name("João")
                        .build());
        assertEquals(Email.DEFAULT_ERROR_EMAIL, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoPasswordNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> User.builder()
                        .id(UserId.of(1L))
                        .tenantId(TenantId.of(1L))
                        .username(Username.of("joao_silva"))
                        .email(Email.of("joao@acme.com"))
                        .name("João")
                        .build());
        assertEquals(Password.DEFAULT_ERROR_PASSWORD, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoNomeNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> User.builder()
                        .id(UserId.of(1L))
                        .tenantId(TenantId.of(1L))
                        .username(Username.of("joao_silva"))
                        .email(Email.of("joao@acme.com"))
                        .password(Password.fromPlainText("senhaSegura123"))
                        .build());
        assertEquals(User.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoNomeEmBranco() {
        DomainException exception = assertThrows(DomainException.class,
                () -> validBuilder().name("   ").build());
        assertEquals(User.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void deveBloquearUsuario() {
        User user = validBuilder().build();
        user.block();
        assertTrue(user.isBlocked());
        assertFalse(user.canLogin());
    }

    @Test
    void deveDesabilitarUsuario() {
        User user = validBuilder().build();
        user.disable();
        assertTrue(user.isDisabled());
        assertFalse(user.canLogin());
    }

    @Test
    void deveAtivarUsuarioBloqueado() {
        User user = validBuilder().status(UserStatus.BLOCKED).build();
        user.activate();
        assertTrue(user.isActive());
        assertTrue(user.canLogin());
    }

    @Test
    void ativacaoDeveSerIdempotente() {
        User user = validBuilder().build();
        user.activate();
        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    void usuarioAtivoPodeLogar() {
        User user = validBuilder().build();
        assertTrue(user.canLogin());
    }

    @Test
    void deveTrocarSenha() {
        User user = validBuilder().build();
        Password novaSenha = Password.fromPlainText("outraSenhaSegura");
        user.changePassword(novaSenha);
        assertTrue(user.verifyPassword("outraSenhaSegura"));
        assertFalse(user.verifyPassword("senhaSegura123"));
    }

    @Test
    void deveLancarExcecaoAoTrocarSenhaParaNula() {
        User user = validBuilder().build();
        DomainException exception = assertThrows(DomainException.class, () -> user.changePassword(null));
        assertEquals(Password.DEFAULT_ERROR_PASSWORD, exception.getMessage());
    }

    @Test
    void deveVerificarSenhaCorreta() {
        User user = validBuilder().build();
        assertTrue(user.verifyPassword("senhaSegura123"));
    }

    @Test
    void deveAtualizarNome() {
        User user = validBuilder().build();
        user.updateName("Outro Nome");
        assertEquals("Outro Nome", user.getName());
    }

    @Test
    void deveLancarExcecaoAoAtualizarNomeParaVazio() {
        User user = validBuilder().build();
        DomainException exception = assertThrows(DomainException.class, () -> user.updateName(""));
        assertEquals(User.ERROR_NAME_REQUIRED, exception.getMessage());
    }

    @Test
    void deveAtualizarEmail() {
        User user = validBuilder().build();
        user.updateEmail(Email.of("novo@acme.com"));
        assertEquals(Email.of("novo@acme.com"), user.getEmail());
    }

    @Test
    void deveLancarExcecaoAoAtualizarEmailParaNulo() {
        User user = validBuilder().build();
        DomainException exception = assertThrows(DomainException.class, () -> user.updateEmail(null));
        assertEquals(Email.DEFAULT_ERROR_EMAIL, exception.getMessage());
    }

    @Test
    void deveConfirmarQuePertenceAoTenant() {
        User user = validBuilder().build();
        assertTrue(user.belongsToTenant(TenantId.of(1L)));
        assertFalse(user.belongsToTenant(TenantId.of(2L)));
    }

    @Test
    void naoDeveBloquearAntesDeAtingirOLimiteDeTentativas() {
        User user = validBuilder().build();
        for (int i = 0; i < User.MAX_FAILED_LOGIN_ATTEMPTS - 1; i++) {
            user.registerFailedLoginAttempt();
        }
        assertFalse(user.isLocked());
        assertTrue(user.canLogin());
        assertEquals(User.MAX_FAILED_LOGIN_ATTEMPTS - 1, user.getFailedLoginAttempts());
    }

    @Test
    void deveBloquearAoAtingirOLimiteDeTentativasFalhas() {
        User user = validBuilder().build();
        for (int i = 0; i < User.MAX_FAILED_LOGIN_ATTEMPTS; i++) {
            user.registerFailedLoginAttempt();
        }
        assertTrue(user.isLocked());
        assertFalse(user.canLogin());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(Instant.now()));
    }

    @Test
    void loginBemSucedidoZeraContadorEBloqueio() {
        User user = validBuilder().build();
        for (int i = 0; i < User.MAX_FAILED_LOGIN_ATTEMPTS; i++) {
            user.registerFailedLoginAttempt();
        }
        assertTrue(user.isLocked());

        user.registerSuccessfulLogin();

        assertFalse(user.isLocked());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
        assertTrue(user.canLogin());
    }

    @Test
    void usuarioComBloqueioExpiradoNaoEstaMaisBloqueado() {
        User user = validBuilder().lockedUntil(Instant.now().minusSeconds(1)).build();
        assertFalse(user.isLocked());
        assertTrue(user.canLogin());
    }
}
