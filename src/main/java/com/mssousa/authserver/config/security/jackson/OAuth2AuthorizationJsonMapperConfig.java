package com.mssousa.authserver.config.security.jackson;

import com.mssousa.authserver.adapter.in.web.security.ClientAwareAuthenticationToken;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.List;

/**
 * {@link JsonMapper} usado por {@code JdbcOAuth2AuthorizationService} (seção 7.3 do plano)
 * para (des)serializar o {@code Authentication} do resource owner guardado em
 * {@code OAuth2Authorization.attributes}. O mapper padrão do Spring Authorization Server
 * não conhece {@link ClientAwareAuthenticationToken} nem {@link AuthenticatedUser} — sem
 * este mapper, a troca do código de autorização por token falha ao reler a authorization
 * persistida (o {@code PolymorphicTypeValidator} rejeita tipos fora do allowlist padrão).
 */
@Configuration
public class OAuth2AuthorizationJsonMapperConfig {

    @Bean
    public JsonMapper oauth2AuthorizationJsonMapper() {
        BasicPolymorphicTypeValidator.Builder ptvBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ClientAwareAuthenticationToken.class.getPackageName())
                .allowIfSubType(AuthenticatedUser.class.getPackageName());

        List<JacksonModule> securityModules = SecurityJacksonModules.getModules(
                OAuth2AuthorizationJsonMapperConfig.class.getClassLoader(), ptvBuilder);

        return JsonMapper.builder()
                .addModules(securityModules)
                .addMixIn(ClientAwareAuthenticationToken.class, ClientAwareAuthenticationTokenMixin.class)
                .addMixIn(AuthenticatedUser.class, AuthenticatedUserMixin.class)
                .addMixIn(UserId.class, ValueObjectMixins.UserIdMixin.class)
                .addMixIn(TenantId.class, ValueObjectMixins.TenantIdMixin.class)
                .addMixIn(SystemId.class, ValueObjectMixins.SystemIdMixin.class)
                .addMixIn(Username.class, ValueObjectMixins.UsernameMixin.class)
                .addMixIn(Email.class, ValueObjectMixins.EmailMixin.class)
                .build();
    }
}
