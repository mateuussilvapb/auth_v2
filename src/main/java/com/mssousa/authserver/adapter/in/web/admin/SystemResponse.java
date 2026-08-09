package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.system.System;

import java.util.List;

/**
 * Corpo de resposta para os endpoints de sistema (seção 9 do plano —
 * {@code /admin/api/v1/tenants/{tenantId}/systems}, {@code /admin/api/v1/systems/{id}}).
 * Nunca inclui o client secret — nem em hash, nem em texto plano (seção 6.6/7.4).
 */
public record SystemResponse(Long id, String clientId, String name, String status,
                              boolean publicClient, List<String> redirectUris, boolean thirdParty) {

    public static SystemResponse from(System system) {
        return new SystemResponse(
                system.getId().value(),
                system.getClientId().value(),
                system.getName(),
                system.getStatus().name(),
                system.isPublicClient(),
                system.getRedirectUris().stream().map(uri -> uri.value()).toList(),
                system.isThirdParty());
    }
}
