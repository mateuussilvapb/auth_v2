package com.mssousa.authserver.adapter.out.security.oauth2;

import com.mssousa.authserver.application.port.out.SystemRepository;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;

/**
 * {@link RegisteredClientRepository} customizado sobre a tabela {@code system}, em vez
 * de {@code oauth2_registered_client} (seção 4.3 e 7.3 do plano). Um {@link System} vira
 * um {@link RegisteredClient} a cada resolução — não há cache; qualquer alteração
 * administrativa (rotação de secret, nova redirect URI) vale imediatamente.
 * <p>
 * Não é {@code @Component}: construído explicitamente por
 * {@code RegisteredClientRepositoryConfig} e combinado com o client estático do console
 * administrativo via {@link CompositeRegisteredClientRepository} — dois beans
 * {@code @Component} do mesmo tipo {@link RegisteredClientRepository} tornariam ambíguo
 * todo ponto de injeção da aplicação.
 * </p>
 */
public class SystemRegisteredClientRepository implements RegisteredClientRepository {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofHours(8);

    private final SystemRepository systemRepository;

    public SystemRegisteredClientRepository(SystemRepository systemRepository) {
        this.systemRepository = systemRepository;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException(
                "Clients são administrados via ManageSystemUseCase (Fase 4), não pelo Authorization Server");
    }

    @Override
    public RegisteredClient findById(String id) {
        try {
            return systemRepository.findById(SystemId.of(Long.parseLong(id)))
                    .map(this::toRegisteredClient)
                    .orElse(null);
        } catch (NumberFormatException | DomainException e) {
            return null;
        }
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        try {
            return systemRepository.findByClientId(ClientId.of(clientId))
                    .map(this::toRegisteredClient)
                    .orElse(null);
        } catch (DomainException e) {
            return null;
        }
    }

    private RegisteredClient toRegisteredClient(System system) {
        RegisteredClient.Builder builder = RegisteredClient.withId(system.getId().value().toString())
                .clientId(system.getClientId().value())
                .clientName(system.getName())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // Perfis (não scopes) carregam a semântica de autorização do projeto
                // (seção 1.2/7.2) — "profile" aqui só existe para o fluxo de consentimento
                // ter algo concreto para exibir/registrar (seção 2.2), não para controlar
                // acesso a recursos.
                .scope("profile");

        system.getRedirectUris().forEach(uri -> builder.redirectUri(uri.value()));

        if (system.isPublicClient()) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else {
            builder.clientSecret(system.getClientSecret().hashedValue())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        }

        return builder
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(system.isThirdParty())
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(ACCESS_TOKEN_TTL)
                        .refreshTokenTimeToLive(REFRESH_TOKEN_TTL)
                        .reuseRefreshTokens(false)
                        .build())
                .build();
    }
}
