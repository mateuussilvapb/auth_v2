package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.application.exception.AccessDeniedException;
import com.mssousa.authserver.application.model.AuthorizedUser;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;

/**
 * Porta de entrada que retorna os códigos de perfil ativos de um usuário num sistema —
 * exatamente o claim {@code profiles} do token (seção 7.2). Chamada após uma
 * autenticação bem-sucedida ({@code AuthenticateUserUseCase}).
 */
public interface AuthorizeUserUseCase {

    /**
     * @throws AccessDeniedException se o usuário não tiver vínculo ativo com o sistema
     */
    AuthorizedUser authorize(TenantId tenantId, UserId userId, SystemId systemId);
}
