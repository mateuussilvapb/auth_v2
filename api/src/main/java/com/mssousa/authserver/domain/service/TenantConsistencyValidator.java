package com.mssousa.authserver.domain.service;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.user.User;

/**
 * Domain Service responsável pela invariante mais crítica do sistema (seção 3.3 do
 * plano): um {@code UserSystem} só pode existir se {@code user.tenantId} for igual ao
 * tenant do sistema (obtido via {@code SystemTenant}).
 * <p>
 * Violar isso significa dar a um usuário de um tenant acesso a um sistema de outro — a
 * falha mais grave possível num sistema multi-tenant. Esta é a camada de domínio da
 * defesa em duas camadas descrita na seção 3.3; a segunda camada são as FKs compostas do
 * banco (seção 4.4), que devem ser mantidas independentemente desta validação.
 * </p>
 */
public class TenantConsistencyValidator {

    public static final String ERROR_TENANT_MISMATCH =
            "Usuário e sistema pertencem a tenants diferentes — vínculo não permitido";

    /**
     * Valida se o usuário e o sistema (via seu vínculo com o tenant) pertencem ao mesmo
     * tenant, condição obrigatória para a criação de um {@code UserSystem}.
     *
     * @throws DomainException se os tenants forem diferentes
     */
    public void validateSameTenant(User user, SystemTenant systemTenant) {
        if (!user.getTenantId().equals(systemTenant.getTenantId())) {
            throw new DomainException(ERROR_TENANT_MISMATCH);
        }
    }
}
