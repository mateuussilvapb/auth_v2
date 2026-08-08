package com.mssousa.authserver.adapter.in.web.auth;

import com.mssousa.authserver.application.model.TenantBranding;

/**
 * Corpo de {@code GET /api/auth/branding} — nome e logo do tenant resolvido pelo
 * {@code client_id}, consumido pela tela de login/consentimento Angular antes de
 * qualquer autenticação (seção 7 do plano).
 */
public record BrandingResponse(String tenantName, String logoUrl) {

    public static BrandingResponse from(TenantBranding branding) {
        return new BrandingResponse(branding.tenantName(), branding.logoUrl());
    }
}
