package com.mssousa.authserver.application.model;

import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Username;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Record de transporte com o resultado de {@code AuthenticatePlatformAdminUseCase} (seção
 * 5 do plano) — análogo a {@link AuthenticatedUser}, mas sem tenant/system (seção 2.1: o
 * platform admin é ortogonal a todo tenant). Usado como principal do {@code Authentication}
 * em vez do agregado {@code PlatformAdmin} completo (que carrega o hash de senha e não
 * deveria ser serializado em {@code OAuth2Authorization.attributes} — ver
 * {@code OAuth2AuthorizationJsonMapperFactory}).
 * <p>
 * Implementa {@link AuthenticatedPrincipal} para que {@code Authentication.getName()}
 * (usado como {@code sub} do JWT emitido — seção 7.2 — e como {@code created_by} via
 * Spring Data JPA Auditing, {@code JpaAuditingConfig}) retorne só o ID, não o
 * {@code toString()} do record inteiro. Sem isso, {@code getName()} caía no
 * {@code toString()} default (dezenas de caracteres: "AuthenticatedPlatformAdmin[...]"),
 * estourando {@code VARCHAR(50)} de {@code created_by} em qualquer tabela auditada — só
 * apareceu num teste manual criando um tenant de verdade via console; os testes de
 * integração nunca pegam porque {@code AbstractRepositoryIntegrationTest.platformAdmin()}
 * constrói o JWT à mão com {@code .subject("1")}, sem passar pelo emissor real de token.
 * </p>
 * <p>
 * Campo chamado {@code displayName}, não {@code name}: um componente de record {@code name}
 * colide com o {@code getName()} exigido por {@link AuthenticatedPrincipal} na introspecção
 * do Jackson usado para persistir {@code OAuth2Authorization.attributes} — mesmo com
 * {@code @JsonIgnore} num dos dois, o Jackson trata os dois acessores como a mesma
 * propriedade lógica "name" e ignora a propriedade inteira (o campo saía {@code null} na
 * releitura). Renomear elimina a ambiguidade na raiz, sem depender de anotação alguma.
 * </p>
 */
public record AuthenticatedPlatformAdmin(
        PlatformAdminId platformAdminId,
        Username username,
        Email email,
        String displayName
) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return platformAdminId.value().toString();
    }
}
