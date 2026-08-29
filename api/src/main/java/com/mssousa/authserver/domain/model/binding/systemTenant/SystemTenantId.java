package com.mssousa.authserver.domain.model.binding.systemTenant;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.shared.DomainId;

/**
 * Value Object representando o identificador único de um vínculo sistema-tenant.
 */
public final class SystemTenantId extends DomainId {

    private SystemTenantId(Long value) {
        super(value);
        validate(value);
    }

    public static SystemTenantId of(Long value) {
        return new SystemTenantId(value);
    }

    private void validate(Long value) {
        if (value <= 0) {
            throw new DomainException("SystemTenantId deve ser um número positivo");
        }
    }
}
