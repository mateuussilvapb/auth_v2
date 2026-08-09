package com.mssousa.authserver.config.security.jackson;

import com.mssousa.authserver.adapter.in.web.security.ClientAwareAuthenticationToken;
import com.mssousa.authserver.application.model.AuthenticatedPlatformAdmin;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.List;

/**
 * Constrói o {@link JsonMapper} usado por {@code JdbcOAuth2AuthorizationService} (seção
 * 7.3 do plano) para (des)serializar o {@code Authentication} do resource owner guardado
 * em {@code OAuth2Authorization.attributes}. O mapper padrão do Spring Authorization
 * Server não conhece {@link ClientAwareAuthenticationToken} nem {@link AuthenticatedUser}
 * — sem este mapper, a troca do código de autorização por token falha ao reler a
 * authorization persistida ({@code PolymorphicTypeValidator} rejeita tipos fora do
 * allowlist padrão).
 * <p>
 * Deliberadamente NÃO é um {@code @Bean}: um {@code JsonMapper} registrado no contexto
 * Spring é adotado pela autoconfiguração do Boot como o {@code JsonMapper} global usado
 * por todos os {@code HttpMessageConverter} do Spring MVC (é o único candidato, então a
 * autoconfiguração do próprio Boot desiste do seu default) — o allowlist restrito do
 * {@code PolymorphicTypeValidator} passaria a quebrar a (des)serialização de qualquer
 * outro controller da aplicação. Descoberto porque a suíte de testes do
 * {@code SystemController} (Fase 8) começou a falhar com erro de tipo polimórfico ao
 * desserializar um {@code List<String>} comum.
 * </p>
 */
public final class OAuth2AuthorizationJsonMapperFactory {

    private OAuth2AuthorizationJsonMapperFactory() {
    }

    public static JsonMapper build() {
        BasicPolymorphicTypeValidator.Builder ptvBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ClientAwareAuthenticationToken.class.getPackageName())
                .allowIfSubType(AuthenticatedUser.class.getPackageName());

        List<JacksonModule> securityModules = SecurityJacksonModules.getModules(
                OAuth2AuthorizationJsonMapperFactory.class.getClassLoader(), ptvBuilder);

        return JsonMapper.builder()
                .addModules(securityModules)
                .addMixIn(ClientAwareAuthenticationToken.class, ClientAwareAuthenticationTokenMixin.class)
                .addMixIn(AuthenticatedUser.class, AuthenticatedUserMixin.class)
                .addMixIn(AuthenticatedPlatformAdmin.class, AuthenticatedPlatformAdminMixin.class)
                .addMixIn(UserId.class, ValueObjectMixins.UserIdMixin.class)
                .addMixIn(TenantId.class, ValueObjectMixins.TenantIdMixin.class)
                .addMixIn(SystemId.class, ValueObjectMixins.SystemIdMixin.class)
                .addMixIn(PlatformAdminId.class, ValueObjectMixins.PlatformAdminIdMixin.class)
                .addMixIn(Username.class, ValueObjectMixins.UsernameMixin.class)
                .addMixIn(Email.class, ValueObjectMixins.EmailMixin.class)
                .build();
    }
}
