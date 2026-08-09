package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;

/**
 * Todos os campos de ID são {@code String} — ver javadoc de {@link TenantResponse} sobre
 * perda de precisão de TSID em {@code Number} do JavaScript.
 */
public record UserSystemResponse(String id, String userId, String systemId, String tenantId, String status) {

    public static UserSystemResponse from(UserSystem binding) {
        return new UserSystemResponse(
                binding.getId().value().toString(),
                binding.getUserId().value().toString(),
                binding.getSystemId().value().toString(),
                binding.getTenantId().value().toString(),
                binding.getStatus().name());
    }
}
