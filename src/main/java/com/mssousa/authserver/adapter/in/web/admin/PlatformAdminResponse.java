package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.platform.PlatformAdmin;

/**
 * Corpo de resposta para os endpoints de platform admin (seção 9 do plano). Nunca inclui
 * o hash da senha (seção 6.6/7.4).
 */
public record PlatformAdminResponse(Long id, String username, String email, String name, String status) {

    public static PlatformAdminResponse from(PlatformAdmin admin) {
        return new PlatformAdminResponse(
                admin.getId().value(),
                admin.getUsername().value(),
                admin.getEmail().value(),
                admin.getName(),
                admin.getStatus().name());
    }
}
