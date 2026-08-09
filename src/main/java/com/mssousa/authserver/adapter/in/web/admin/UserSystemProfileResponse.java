package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;

/**
 * Todos os campos de ID são {@code String} — ver javadoc de {@link TenantResponse} sobre
 * perda de precisão de TSID em {@code Number} do JavaScript.
 */
public record UserSystemProfileResponse(String id, String userSystemId, String systemProfileId, String status) {

    public static UserSystemProfileResponse from(UserSystemProfile binding) {
        return new UserSystemProfileResponse(
                binding.getId().value().toString(),
                binding.getUserSystemId().value().toString(),
                binding.getSystemProfileId().value().toString(),
                binding.getStatus().name());
    }
}
