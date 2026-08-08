package com.mssousa.authserver.domain.model.profile;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.shared.DomainId;

/**
 * Value Object representando o identificador único de um perfil de sistema.
 */
public final class SystemProfileId extends DomainId {

    private SystemProfileId(Long value) {
        super(value);
        validate(value);
    }

    public static SystemProfileId of(Long value) {
        return new SystemProfileId(value);
    }

    private void validate(Long value) {
        if (value <= 0) {
            throw new DomainException("SystemProfileId deve ser um número positivo");
        }
    }
}
