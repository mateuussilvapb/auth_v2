package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotNull;

public record BindProfileRequest(@NotNull Long profileId) {
}
