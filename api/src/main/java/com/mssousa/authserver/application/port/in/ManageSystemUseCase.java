package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Porta de entrada para administração de sistemas (seção 9:
 * {@code /admin/api/v1/tenants/{tenantId}/systems}, {@code /admin/api/v1/systems/{id}}).
 * <p>
 * Criar um sistema já cria o vínculo {@code SystemTenant} (cardinalidade 1:1, decisão
 * D3) — não existe um caminho separado para "desvincular" um sistema do seu tenant.
 * </p>
 */
public interface ManageSystemUseCase {

    System createSystem(TenantId tenantId, String clientId, String name, boolean publicClient,
                         String clientSecret, List<String> initialRedirectUris, boolean thirdParty);

    System updateSystem(SystemId id, String newName);

    System activateSystem(SystemId id);

    System deactivateSystem(SystemId id);

    System addRedirectUri(SystemId id, String uri);

    System removeRedirectUri(SystemId id, String uri);

    System rotateSecret(SystemId id, String newSecret);

    System getSystem(SystemId id);

    Page<System> listSystemsByTenant(TenantId tenantId, Pageable pageable);
}
