package com.mssousa.authserver.domain.service;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;

/**
 * Domain Service que encapsula a regra de negócio em torno do Platform Admin que não
 * pertence a uma única instância: o sistema nunca pode ficar sem nenhum platform admin
 * ativo, sob pena de ninguém conseguir mais administrar os tenants (seção 2.1 do plano).
 */
public class PlatformAdminPolicy {

    public static final String ERROR_LAST_ACTIVE_ADMIN =
            "Não é possível desativar o último platform admin ativo";

    /**
     * Valida se um platform admin pode ser desativado, dado o total de platform admins
     * ativos no momento (incluindo o próprio alvo, se ainda estiver ativo).
     *
     * @param target             platform admin que se deseja desativar
     * @param activeAdminsCount  quantidade total de platform admins com status ACTIVE
     * @throws DomainException se o alvo for o único platform admin ativo
     */
    public void validateCanDeactivate(PlatformAdmin target, long activeAdminsCount) {
        if (target.isActive() && activeAdminsCount <= 1) {
            throw new DomainException(ERROR_LAST_ACTIVE_ADMIN);
        }
    }
}
