package com.mssousa.authserver.domain.model.profile;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.system.SystemId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemProfileTest {

    private SystemProfile.Builder validBuilder() {
        return SystemProfile.builder()
                .id(SystemProfileId.of(1L))
                .systemId(SystemId.of(1L))
                .code(ProfileCode.of("ADMIN"))
                .description("Administrador do sistema");
    }

    @Test
    void deveCriarPerfilValidoComStatusPadraoActive() {
        SystemProfile profile = validBuilder().build();

        assertEquals(SystemProfileId.of(1L), profile.getId());
        assertEquals(SystemId.of(1L), profile.getSystemId());
        assertEquals(ProfileCode.of("ADMIN"), profile.getCode());
        assertEquals("Administrador do sistema", profile.getDescription());
        assertTrue(profile.isActive());
    }

    @Test
    void deveLancarExcecaoQuandoIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> SystemProfile.builder().systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN")).build());
        assertEquals(SystemProfile.ERROR_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSystemIdNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> SystemProfile.builder().id(SystemProfileId.of(1L)).code(ProfileCode.of("ADMIN")).build());
        assertEquals(SystemProfile.ERROR_SYSTEM_ID_REQUIRED, exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoCodeNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> SystemProfile.builder().id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).build());
        assertEquals(ProfileCode.ERROR_REQUIRED, exception.getMessage());
    }

    @Test
    void deveAtivarEDesativarPerfil() {
        SystemProfile profile = validBuilder().build();
        profile.deactivate();
        assertFalse(profile.isActive());

        profile.activate();
        assertTrue(profile.isActive());
    }

    @Test
    void ativacaoDeveSerIdempotente() {
        SystemProfile profile = validBuilder().build();
        profile.activate();
        profile.activate();
        assertTrue(profile.isActive());
    }

    @Test
    void deveAtualizarDescricao() {
        SystemProfile profile = validBuilder().build();
        profile.updateDescription("Nova descrição");
        assertEquals("Nova descrição", profile.getDescription());
    }

    @Test
    void deveConfirmarQuePertenceAoSistema() {
        SystemProfile profile = validBuilder().build();
        assertTrue(profile.belongsTo(SystemId.of(1L)));
        assertFalse(profile.belongsTo(SystemId.of(2L)));
    }

    @Test
    void mesmoCodigoPodeExistirEmSistemasDiferentes() {
        SystemProfile perfilSistemaA = SystemProfile.builder()
                .id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN")).build();
        SystemProfile perfilSistemaB = SystemProfile.builder()
                .id(SystemProfileId.of(2L)).systemId(SystemId.of(2L)).code(ProfileCode.of("ADMIN")).build();

        assertEquals(perfilSistemaA.getCode(), perfilSistemaB.getCode());
        assertNotEquals(perfilSistemaA.getSystemId(), perfilSistemaB.getSystemId());
    }
}
