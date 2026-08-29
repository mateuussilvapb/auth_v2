package com.mssousa.authserver.config.security;

import com.mssousa.authserver.adapter.out.security.oauth2.CompositeRegisteredClientRepository;
import com.mssousa.authserver.adapter.out.security.oauth2.SystemRegisteredClientRepository;
import com.mssousa.authserver.application.port.out.SystemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Monta o {@link RegisteredClientRepository} único da aplicação, combinando os clients
 * tenant-scoped ({@link SystemRegisteredClientRepository}, sobre a tabela {@code system})
 * com o client estático do console administrativo Angular — que não é um {@code System}
 * porque não pertence a nenhum tenant (D3/D9 do plano exigem 1 sistema : 1 tenant; o
 * console precisa autenticar platform admins, que são deliberadamente ortogonais a todo
 * tenant, seção 2.1).
 */
@Configuration
public class RegisteredClientRepositoryConfig {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofHours(8);

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            SystemRepository systemRepository,
            @Value("${authserver.console-client.client-id}") String consoleClientId,
            @Value("${authserver.console-client.redirect-uris}") String consoleRedirectUris) {

        SystemRegisteredClientRepository systemRegisteredClientRepository =
                new SystemRegisteredClientRepository(systemRepository);

        InMemoryRegisteredClientRepository consoleRegisteredClientRepository =
                new InMemoryRegisteredClientRepository(consoleClient(consoleClientId, consoleRedirectUris));

        return new CompositeRegisteredClientRepository(
                consoleRegisteredClientRepository, systemRegisteredClientRepository);
    }

    private RegisteredClient consoleClient(String clientId, String redirectUrisCsv) {
        List<String> redirectUris = Arrays.stream(redirectUrisCsv.split(","))
                .map(String::trim)
                .filter(uri -> !uri.isBlank())
                .toList();

        // ID fixo (igual ao clientId) em vez de UUID por instância — evita que um restart
        // do processo troque o registered_client_id referenciado por authorizations e
        // consents já persistidos em oauth2_authorization (JdbcOAuth2AuthorizationService).
        RegisteredClient.Builder builder = RegisteredClient.withId(clientId)
                .clientId(clientId)
                .clientName("Console administrativo")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // Mesma justificativa de SystemRegisteredClientRepository: o scope só existe
                // para o Authorization Server ter algo concreto no pedido; autorização real
                // vem do claim platform_admin (JwtTokenCustomizer), não de scope OAuth2.
                .scope("profile");

        redirectUris.forEach(builder::redirectUri);

        return builder
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(ACCESS_TOKEN_TTL)
                        .refreshTokenTimeToLive(REFRESH_TOKEN_TTL)
                        .reuseRefreshTokens(false)
                        .build())
                .build();
    }
}
