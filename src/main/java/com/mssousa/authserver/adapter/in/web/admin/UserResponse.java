package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.user.User;

/**
 * Corpo de resposta para os endpoints de usuário (seção 9 do plano). Nunca inclui o hash
 * da senha (seção 6.6/7.4).
 */
public record UserResponse(Long id, Long tenantId, String username, String email, String name, String status) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getTenantId().value(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getName(),
                user.getStatus().name());
    }
}
