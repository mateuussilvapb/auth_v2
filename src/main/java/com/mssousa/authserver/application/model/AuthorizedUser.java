package com.mssousa.authserver.application.model;

import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;

import java.util.List;

/**
 * Record de transporte com o resultado de {@code AuthorizeUserUseCase} — os códigos de
 * perfil ativos do usuário no sistema, exatamente o que entra no claim {@code profiles}
 * do token (seção 7.2). Nada além de códigos: sem permissões, menus ou ações (seção 1.2).
 */
public record AuthorizedUser(
        UserId userId,
        TenantId tenantId,
        SystemId systemId,
        List<String> profileCodes
) {
}
