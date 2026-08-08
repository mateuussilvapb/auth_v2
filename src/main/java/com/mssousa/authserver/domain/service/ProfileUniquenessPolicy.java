package com.mssousa.authserver.domain.service;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;

import java.util.List;

/**
 * Domain Service que garante {@code UNIQUE (systemId, code)} (seção 3.2 do plano) antes
 * da persistência — a unicidade real é imposta pelo banco, mas validar aqui produz um
 * erro de negócio claro em vez de uma violação de constraint SQL.
 * <p>
 * Recebe a lista de perfis já existentes **do mesmo sistema**; comparar entre sistemas
 * diferentes não é responsabilidade desta classe — códigos repetem entre sistemas por
 * design.
 * </p>
 */
public class ProfileUniquenessPolicy {

    public static final String ERROR_DUPLICATE_CODE =
            "Já existe um perfil com este código neste sistema";

    /**
     * Valida a unicidade do código para um novo perfil.
     */
    public void validateUniqueForCreate(ProfileCode code, List<SystemProfile> existingProfilesInSystem) {
        boolean duplicate = existingProfilesInSystem.stream()
                .anyMatch(profile -> profile.getCode().equals(code));
        if (duplicate) {
            throw new DomainException(ERROR_DUPLICATE_CODE);
        }
    }

    /**
     * Valida a unicidade do código ao atualizar um perfil existente, ignorando o
     * próprio perfil na comparação.
     */
    public void validateUniqueForUpdate(SystemProfileId excludingId, ProfileCode code, List<SystemProfile> existingProfilesInSystem) {
        boolean duplicate = existingProfilesInSystem.stream()
                .filter(profile -> !profile.getId().equals(excludingId))
                .anyMatch(profile -> profile.getCode().equals(code));
        if (duplicate) {
            throw new DomainException(ERROR_DUPLICATE_CODE);
        }
    }
}
