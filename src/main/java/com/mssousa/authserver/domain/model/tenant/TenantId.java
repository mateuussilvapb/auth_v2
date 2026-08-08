package com.mssousa.authserver.domain.model.tenant;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.shared.DomainId;

/**
 * Value Object representando o identificador único de um tenant.
 */
public final class TenantId extends DomainId {

    private TenantId(Long value) {
        super(value);
        validate(value);
    }

    public static TenantId of(Long value) {
        return new TenantId(value);
    }

    private void validate(Long value) {
        if (value <= 0) {
            throw new DomainException("TenantId deve ser um número positivo");
        }
    }
}
