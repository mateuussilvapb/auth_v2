package com.mssousa.authserver.domain.model.platform;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.shared.DomainId;

/**
 * Value Object representando o identificador único de um platform admin.
 */
public final class PlatformAdminId extends DomainId {

    private PlatformAdminId(Long value) {
        super(value);
        validate(value);
    }

    public static PlatformAdminId of(Long value) {
        return new PlatformAdminId(value);
    }

    private void validate(Long value) {
        if (value <= 0) {
            throw new DomainException("PlatformAdminId deve ser um número positivo");
        }
    }
}
