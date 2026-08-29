package com.mssousa.authserver.domain.model.binding.userSystem;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.shared.DomainId;

/**
 * Value Object representando o identificador único de um vínculo usuário-sistema.
 */
public final class UserSystemId extends DomainId {

    private UserSystemId(Long value) {
        super(value);
        validate(value);
    }

    public static UserSystemId of(Long value) {
        return new UserSystemId(value);
    }

    private void validate(Long value) {
        if (value <= 0) {
            throw new DomainException("UserSystemId deve ser um número positivo");
        }
    }
}
