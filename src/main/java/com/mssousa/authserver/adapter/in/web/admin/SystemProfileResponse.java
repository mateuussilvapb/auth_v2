package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.profile.SystemProfile;

public record SystemProfileResponse(Long id, Long systemId, String code, String description, String status) {

    public static SystemProfileResponse from(SystemProfile profile) {
        return new SystemProfileResponse(
                profile.getId().value(),
                profile.getSystemId().value(),
                profile.getCode().value(),
                profile.getDescription(),
                profile.getStatus().name());
    }
}
