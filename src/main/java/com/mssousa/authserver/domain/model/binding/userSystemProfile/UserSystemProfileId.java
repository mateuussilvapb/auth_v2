package com.mssousa.authserver.domain.model.binding.userSystemProfile;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.shared.DomainId;

/**
 * Value Object representando o identificador único de um vínculo usuário-perfil.
 */
public final class UserSystemProfileId extends DomainId {

    private UserSystemProfileId(Long value) {
        super(value);
        validate(value);
    }

    public static UserSystemProfileId of(Long value) {
        return new UserSystemProfileId(value);
    }

    private void validate(Long value) {
        if (value <= 0) {
            throw new DomainException("UserSystemProfileId deve ser um número positivo");
        }
    }
}
