package com.mssousa.authserver.domain.model.system;

import com.mssousa.authserver.domain.exception.DomainException;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidade de domínio representando um Sistema satélite (client OAuth2).
 * <p>
 * Vinculado a exatamente 1 tenant através do binding {@code SystemTenant} (decisão D3,
 * modelado na Fase 2). Possui ao menos uma {@link RedirectUri}. Clients públicos (SPA)
 * não têm {@code clientSecret}; clients confidenciais exigem um.
 * </p>
 */
public class System {

    public static final String ERROR_ID_REQUIRED = "ID do sistema não pode ser nulo";
    public static final String ERROR_NAME_REQUIRED = "Nome do sistema não pode ser nulo ou vazio";
    public static final String ERROR_STATUS_REQUIRED = "Status do sistema não pode ser nulo";
    public static final String ERROR_REDIRECT_URI_REQUIRED = "Sistema deve ter ao menos uma redirect URI";
    public static final String ERROR_SECRET_REQUIRED_FOR_CONFIDENTIAL = "Client Secret é obrigatório para clients confidenciais";
    public static final String ERROR_SECRET_NOT_ALLOWED_FOR_PUBLIC = "Client público não pode ter Client Secret";
    public static final String ERROR_LAST_REDIRECT_URI = "Sistema deve manter ao menos uma redirect URI";

    private final SystemId id;
    private final ClientId clientId;
    private ClientSecret clientSecret;
    private String name;
    private final boolean publicClient;
    private final List<RedirectUri> redirectUris;
    private SystemStatus status;

    private System(Builder builder) {
        this.id = builder.id;
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.name = builder.name;
        this.publicClient = builder.publicClient;
        this.redirectUris = new ArrayList<>(builder.redirectUris);
        this.status = builder.status;

        validate();
    }

    private void validate() {
        if (id == null) {
            throw new DomainException(ERROR_ID_REQUIRED);
        }
        if (clientId == null) {
            throw new DomainException(ClientId.ERROR_REQUIRED);
        }
        if (name == null || name.isBlank()) {
            throw new DomainException(ERROR_NAME_REQUIRED);
        }
        if (redirectUris.isEmpty()) {
            throw new DomainException(ERROR_REDIRECT_URI_REQUIRED);
        }
        if (status == null) {
            throw new DomainException(ERROR_STATUS_REQUIRED);
        }
        validateSecretConsistency();
    }

    private void validateSecretConsistency() {
        if (publicClient && clientSecret != null) {
            throw new DomainException(ERROR_SECRET_NOT_ALLOWED_FOR_PUBLIC);
        }
        if (!publicClient && clientSecret == null) {
            throw new DomainException(ERROR_SECRET_REQUIRED_FOR_CONFIDENTIAL);
        }
    }

    public SystemId getId() {
        return id;
    }

    public ClientId getClientId() {
        return clientId;
    }

    public ClientSecret getClientSecret() {
        return clientSecret;
    }

    public String getName() {
        return name;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public List<RedirectUri> getRedirectUris() {
        return List.copyOf(redirectUris);
    }

    public SystemStatus getStatus() {
        return status;
    }

    /**
     * Ativa o sistema, permitindo novas autenticações. Operação idempotente.
     */
    public void activate() {
        this.status = SystemStatus.ACTIVE;
    }

    /**
     * Desativa o sistema, impedindo novas autenticações. Operação idempotente.
     */
    public void deactivate() {
        this.status = SystemStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == SystemStatus.ACTIVE;
    }

    public boolean canAcceptAuthentication() {
        return this.status == SystemStatus.ACTIVE;
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new DomainException(ERROR_NAME_REQUIRED);
        }
        this.name = newName;
    }

    /**
     * Rotaciona o client secret. Só é permitido para clients confidenciais.
     *
     * @throws DomainException se o sistema for um client público
     */
    public void rotateSecret(ClientSecret newSecret) {
        if (publicClient) {
            throw new DomainException(ERROR_SECRET_NOT_ALLOWED_FOR_PUBLIC);
        }
        if (newSecret == null) {
            throw new DomainException(ERROR_SECRET_REQUIRED_FOR_CONFIDENTIAL);
        }
        this.clientSecret = newSecret;
    }

    public void addRedirectUri(RedirectUri redirectUri) {
        if (redirectUri == null) {
            throw new DomainException(RedirectUri.ERROR_REQUIRED);
        }
        if (!redirectUris.contains(redirectUri)) {
            redirectUris.add(redirectUri);
        }
    }

    /**
     * Remove uma redirect URI, desde que não seja a última do sistema.
     *
     * @throws DomainException se a remoção deixar o sistema sem nenhuma redirect URI
     */
    public void removeRedirectUri(RedirectUri redirectUri) {
        if (redirectUris.size() <= 1 && redirectUris.contains(redirectUri)) {
            throw new DomainException(ERROR_LAST_REDIRECT_URI);
        }
        redirectUris.remove(redirectUri);
    }

    public boolean matchesRedirectUri(String providedRedirectUri) {
        if (providedRedirectUri == null) {
            return false;
        }
        return redirectUris.stream().anyMatch(uri -> uri.value().equals(providedRedirectUri));
    }

    /**
     * Valida se o client secret fornecido corresponde ao registrado.
     * Sempre falso para clients públicos, que não têm secret.
     */
    public boolean verifyClientSecret(String providedSecret) {
        if (publicClient || providedSecret == null || clientSecret == null) {
            return false;
        }
        return this.clientSecret.matches(providedSecret);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SystemId id;
        private ClientId clientId;
        private ClientSecret clientSecret;
        private String name;
        private boolean publicClient = true;
        private final List<RedirectUri> redirectUris = new ArrayList<>();
        private SystemStatus status = SystemStatus.ACTIVE;

        public Builder id(SystemId id) {
            this.id = id;
            return this;
        }

        public Builder clientId(ClientId clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(ClientSecret clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder publicClient(boolean publicClient) {
            this.publicClient = publicClient;
            return this;
        }

        public Builder redirectUri(RedirectUri redirectUri) {
            this.redirectUris.add(redirectUri);
            return this;
        }

        public Builder redirectUris(List<RedirectUri> redirectUris) {
            this.redirectUris.clear();
            if (redirectUris != null) {
                this.redirectUris.addAll(redirectUris);
            }
            return this;
        }

        public Builder status(SystemStatus status) {
            this.status = status != null ? status : SystemStatus.ACTIVE;
            return this;
        }

        public System build() {
            if (id == null) {
                throw new DomainException(System.ERROR_ID_REQUIRED);
            }
            if (clientId == null) {
                throw new DomainException(ClientId.ERROR_REQUIRED);
            }
            if (name == null || name.isBlank()) {
                throw new DomainException(System.ERROR_NAME_REQUIRED);
            }
            if (redirectUris.isEmpty()) {
                throw new DomainException(System.ERROR_REDIRECT_URI_REQUIRED);
            }
            if (status == null) {
                throw new DomainException(System.ERROR_STATUS_REQUIRED);
            }

            return new System(this);
        }
    }
}
