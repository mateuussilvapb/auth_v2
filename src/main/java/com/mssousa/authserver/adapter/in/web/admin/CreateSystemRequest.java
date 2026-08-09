package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateSystemRequest(
        @NotBlank String clientId,
        @NotBlank String name,
        boolean publicClient,
        String clientSecret,
        @NotEmpty List<@NotBlank String> initialRedirectUris,
        boolean thirdParty) {
}
