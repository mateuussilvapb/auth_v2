package com.mssousa.authserver.application.model;

import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;

/**
 * Record de transporte com o resultado de {@code AuthenticateUserUseCase} (seção 5 do
 * plano) — identidade do usuário já validada pela cascata completa de status (3.4),
 * dentro do tenant e sistema resolvidos a partir do {@code client_id}.
 */
public record AuthenticatedUser(
        UserId userId,
        TenantId tenantId,
        SystemId systemId,
        Username username,
        Email email,
        String name
) {
}
