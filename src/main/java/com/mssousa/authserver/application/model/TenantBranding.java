package com.mssousa.authserver.application.model;

/**
 * Branding público de um tenant (seção 7 do plano) — o suficiente para a SPA Angular
 * customizar a tela de login/consentimento antes de qualquer autenticação.
 */
public record TenantBranding(String tenantName, String logoUrl) {
}
