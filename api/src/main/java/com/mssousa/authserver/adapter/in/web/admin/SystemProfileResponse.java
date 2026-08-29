package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.profile.SystemProfile;

/**
 * {@code id}/{@code systemId} são {@code String} — ver javadoc de {@link TenantResponse}
 * sobre perda de precisão de TSID em {@code Number} do JavaScript.
 */
public record SystemProfileResponse(String id, String systemId, String code, String description, String status) {

    public static SystemProfileResponse from(SystemProfile profile) {
        return new SystemProfileResponse(
                profile.getId().value().toString(),
                profile.getSystemId().value().toString(),
                profile.getCode().value(),
                profile.getDescription(),
                profile.getStatus().name());
    }
}
