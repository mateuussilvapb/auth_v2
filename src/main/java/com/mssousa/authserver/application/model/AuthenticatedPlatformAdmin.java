package com.mssousa.authserver.application.model;

import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Username;

/**
 * Record de transporte com o resultado de {@code AuthenticatePlatformAdminUseCase} (seção
 * 5 do plano) — análogo a {@link AuthenticatedUser}, mas sem tenant/system (seção 2.1: o
 * platform admin é ortogonal a todo tenant). Usado como principal do {@code Authentication}
 * em vez do agregado {@code PlatformAdmin} completo (que carrega o hash de senha e não
 * deveria ser serializado em {@code OAuth2Authorization.attributes} — ver
 * {@code OAuth2AuthorizationJsonMapperFactory}).
 */
public record AuthenticatedPlatformAdmin(
        PlatformAdminId platformAdminId,
        Username username,
        Email email,
        String name
) {
}
