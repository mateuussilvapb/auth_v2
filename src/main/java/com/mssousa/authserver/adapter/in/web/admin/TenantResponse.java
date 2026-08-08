package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.tenant.Tenant;

/**
 * Corpo de resposta para os endpoints de tenant (seção 9 do plano — {@code /admin/api/v1/tenants}).
 */
public record TenantResponse(Long id, String code, String name, String status, String logoUrl) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId().value(),
                tenant.getCode().value(),
                tenant.getName(),
                tenant.getStatus().name(),
                tenant.getLogoUrl());
    }
}
