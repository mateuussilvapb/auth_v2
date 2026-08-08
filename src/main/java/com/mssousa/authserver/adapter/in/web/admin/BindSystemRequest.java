package com.mssousa.authserver.adapter.in.web.admin;

import jakarta.validation.constraints.NotNull;

public record BindSystemRequest(@NotNull Long systemId) {
}
