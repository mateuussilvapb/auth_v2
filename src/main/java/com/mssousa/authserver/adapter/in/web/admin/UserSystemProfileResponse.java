package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;

public record UserSystemProfileResponse(Long id, Long userSystemId, Long systemProfileId, String status) {

    public static UserSystemProfileResponse from(UserSystemProfile binding) {
        return new UserSystemProfileResponse(
                binding.getId().value(),
                binding.getUserSystemId().value(),
                binding.getSystemProfileId().value(),
                binding.getStatus().name());
    }
}
