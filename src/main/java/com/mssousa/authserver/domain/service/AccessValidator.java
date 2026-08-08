package com.mssousa.authserver.domain.service;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.User;

/**
 * Domain Service que aplica a cascata completa de status descrita na seção 3.4 do plano:
 * o acesso só é concedido se **todos** os níveis estiverem ativos. Qualquer um inativo
 * interrompe a cadeia.
 * <p>
 * Dividido em dois métodos porque correspondem a momentos distintos do fluxo (seção 7.1
 * e Fase 5): {@link #validateLoginAccess} cobre até o vínculo usuário-sistema (usado por
 * {@code AuthenticateUserUseCase}); {@link #validateProfileAccess} cobre o vínculo
 * usuário-perfil e o próprio perfil (usado por {@code AuthorizeUserUseCase}, um perfil de
 * cada vez, já que um usuário pode ter vários perfis e apenas os ativos entram no token).
 * </p>
 */
public class AccessValidator {

    public static final String ERROR_TENANT_INACTIVE = "Tenant está inativo";
    public static final String ERROR_SYSTEM_INACTIVE = "Sistema está inativo";
    public static final String ERROR_USER_INACTIVE = "Usuário está inativo ou bloqueado";
    public static final String ERROR_USER_LOCKED = "Usuário temporariamente bloqueado por excesso de tentativas de login";
    public static final String ERROR_PROFILE_INACTIVE = "Perfil está inativo";

    /**
     * Valida a cascata: tenant → sistema → vínculo sistema-tenant → usuário → vínculo
     * usuário-sistema.
     *
     * @throws DomainException no primeiro nível inativo encontrado
     */
    public void validateLoginAccess(Tenant tenant, System system, SystemTenant systemTenant, User user, UserSystem userSystem) {
        if (!tenant.isActive()) {
            throw new DomainException(ERROR_TENANT_INACTIVE);
        }
        if (!system.isActive()) {
            throw new DomainException(ERROR_SYSTEM_INACTIVE);
        }
        systemTenant.validateAccess();
        if (!user.isActive()) {
            throw new DomainException(ERROR_USER_INACTIVE);
        }
        if (user.isLocked()) {
            throw new DomainException(ERROR_USER_LOCKED);
        }
        userSystem.validateAccess();
    }

    /**
     * Valida a cascata restante para um perfil específico: vínculo usuário-perfil → perfil.
     *
     * @throws DomainException se o vínculo ou o perfil estiverem inativos
     */
    public void validateProfileAccess(UserSystemProfile userSystemProfile, SystemProfile profile) {
        userSystemProfile.validateAccess();
        if (!profile.isActive()) {
            throw new DomainException(ERROR_PROFILE_INACTIVE);
        }
    }

    /**
     * Verifica a cascata de login sem lançar exceção.
     */
    public boolean canLogin(Tenant tenant, System system, SystemTenant systemTenant, User user, UserSystem userSystem) {
        try {
            validateLoginAccess(tenant, system, systemTenant, user, userSystem);
            return true;
        } catch (DomainException e) {
            return false;
        }
    }
}
