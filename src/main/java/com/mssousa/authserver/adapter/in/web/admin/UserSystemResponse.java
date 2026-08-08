package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;

public record UserSystemResponse(Long id, Long userId, Long systemId, Long tenantId, String status) {

    public static UserSystemResponse from(UserSystem binding) {
        return new UserSystemResponse(
                binding.getId().value(),
                binding.getUserId().value(),
                binding.getSystemId().value(),
                binding.getTenantId().value(),
                binding.getStatus().name());
    }
}
