package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.domain.model.tenant.Tenant;

/**
 * Corpo de resposta para os endpoints de tenant (seção 9 do plano — {@code /admin/api/v1/tenants}).
 * <p>
 * {@code id} é {@code String}, não {@code Long}: TSIDs (seção 4.2 do plano) regularmente
 * excedem {@code Number.MAX_SAFE_INTEGER} do JavaScript (2^53-1) — serializados como
 * número JSON, o console Angular perde precisão ao decodificá-los (confirmado num teste
 * manual: {@code 874316167430144221} virou {@code 874316167430144300}), quebrando toda
 * operação subsequente que dependa do ID exato.
 * </p>
 */
public record TenantResponse(String id, String code, String name, String status, String logoUrl) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId().value().toString(),
                tenant.getCode().value(),
                tenant.getName(),
                tenant.getStatus().name(),
                tenant.getLogoUrl());
    }
}
