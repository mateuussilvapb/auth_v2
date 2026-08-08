package com.mssousa.authserver.adapter.in.web.common;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo reutilizável para os endpoints {@code PATCH .../status} da API administrativa
 * (seção 9 do plano) — cada recurso interpreta o valor de acordo com seu próprio enum de
 * status (ex: {@code ACTIVE}/{@code INACTIVE} para Tenant, mais {@code BLOCKED} para User).
 */
public record UpdateStatusRequest(@NotBlank String status) {
}
