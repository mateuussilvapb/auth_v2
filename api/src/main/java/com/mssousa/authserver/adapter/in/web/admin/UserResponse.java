package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.user.User;

/**
 * Corpo de resposta para os endpoints de usuário (seção 9 do plano). Nunca inclui o hash
 * da senha (seção 6.6/7.4).
 * <p>
 * {@code id}/{@code tenantId} são {@code String} — ver javadoc de {@link TenantResponse}
 * sobre perda de precisão de TSID em {@code Number} do JavaScript.
 * </p>
 */
public record UserResponse(String id, String tenantId, String username, String email, String name, String status) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().value().toString(),
                user.getTenantId().value().toString(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getName(),
                user.getStatus().name());
    }
}
