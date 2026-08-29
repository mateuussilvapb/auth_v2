package com.mssousa.authserver.domain.service;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileUniquenessPolicyTest {

    private final ProfileUniquenessPolicy policy = new ProfileUniquenessPolicy();

    private SystemProfile profile(long id, String code) {
        return SystemProfile.builder()
                .id(SystemProfileId.of(id))
                .systemId(SystemId.of(1L))
                .code(ProfileCode.of(code))
                .build();
    }

    @Test
    void devePermitirCriarQuandoCodigoNaoExiste() {
        List<SystemProfile> existentes = List.of(profile(1L, "ADMIN"));
        assertDoesNotThrow(() -> policy.validateUniqueForCreate(ProfileCode.of("FINANCEIRO"), existentes));
    }

    @Test
    void deveLancarExcecaoAoCriarComCodigoDuplicado() {
        List<SystemProfile> existentes = List.of(profile(1L, "ADMIN"));

        DomainException exception = assertThrows(DomainException.class,
                () -> policy.validateUniqueForCreate(ProfileCode.of("ADMIN"), existentes));
        assertEquals(ProfileUniquenessPolicy.ERROR_DUPLICATE_CODE, exception.getMessage());
    }

    @Test
    void devePermitirListaVazia() {
        assertDoesNotThrow(() -> policy.validateUniqueForCreate(ProfileCode.of("ADMIN"), List.of()));
    }

    @Test
    void devePermitirAtualizarMantendoOMesmoCodigo() {
        List<SystemProfile> existentes = List.of(profile(1L, "ADMIN"));
        assertDoesNotThrow(() -> policy.validateUniqueForUpdate(SystemProfileId.of(1L), ProfileCode.of("ADMIN"), existentes));
    }

    @Test
    void deveLancarExcecaoAoAtualizarParaCodigoDeOutroPerfil() {
        List<SystemProfile> existentes = List.of(profile(1L, "ADMIN"), profile(2L, "FINANCEIRO"));

        DomainException exception = assertThrows(DomainException.class,
                () -> policy.validateUniqueForUpdate(SystemProfileId.of(1L), ProfileCode.of("FINANCEIRO"), existentes));
        assertEquals(ProfileUniquenessPolicy.ERROR_DUPLICATE_CODE, exception.getMessage());
    }
}
