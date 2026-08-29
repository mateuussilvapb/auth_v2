package com.mssousa.authserver.domain.model.tenant;

import com.mssousa.authserver.domain.exception.DomainException;

/**
 * Entidade de domínio representando um Tenant (organização cliente).
 * <p>
 * O tenant é a fronteira de isolamento de dados do sistema: todo usuário, sistema e
 * vínculo pertence a exatamente um tenant. O código é imutável após a criação.
 * </p>
 */
public class Tenant {

    public static final String ERROR_ID_REQUIRED = "ID do tenant não pode ser nulo";
    public static final String ERROR_CODE_REQUIRED = "Código do tenant não pode ser nulo";
    public static final String ERROR_NAME_REQUIRED = "Nome do tenant não pode ser nulo ou vazio";
    public static final String ERROR_STATUS_REQUIRED = "Status do tenant não pode ser nulo";

    private final TenantId id;
    private final TenantCode code;
    private String name;
    private TenantStatus status;
    private String logoUrl;

    private Tenant(Builder builder) {
        this.id = builder.id;
        this.code = builder.code;
        this.name = builder.name;
        this.status = builder.status;
        this.logoUrl = builder.logoUrl;

        validate();
    }

    private void validate() {
        if (id == null) {
            throw new DomainException(ERROR_ID_REQUIRED);
        }
        if (code == null) {
            throw new DomainException(ERROR_CODE_REQUIRED);
        }
        if (name == null || name.isBlank()) {
            throw new DomainException(ERROR_NAME_REQUIRED);
        }
        if (status == null) {
            throw new DomainException(ERROR_STATUS_REQUIRED);
        }
    }

    public TenantId getId() {
        return id;
    }

    public TenantCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    /**
     * URL do logo exibido na tela de login/consentimento do SPA Angular (seção 7 do
     * plano: "branding por tenant resolvido pelo client_id"). Opcional — {@code null}
     * quando o tenant não tem logo configurado.
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Ativa o tenant, permitindo login em todos os seus sistemas.
     * Operação idempotente.
     */
    public void activate() {
        this.status = TenantStatus.ACTIVE;
    }

    /**
     * Desativa o tenant, bloqueando login em todos os seus sistemas (cascata da seção 3.4).
     * Operação idempotente.
     */
    public void deactivate() {
        this.status = TenantStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
    }

    /**
     * Atualiza o nome do tenant.
     *
     * @param newName novo nome
     * @throws DomainException se o novo nome for nulo ou vazio
     */
    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new DomainException(ERROR_NAME_REQUIRED);
        }
        this.name = newName;
    }

    /**
     * Atualiza o logo. {@code null} remove o logo (volta ao branding padrão do Angular).
     */
    public void updateLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private TenantId id;
        private TenantCode code;
        private String name;
        private TenantStatus status = TenantStatus.ACTIVE;
        private String logoUrl;

        public Builder id(TenantId id) {
            this.id = id;
            return this;
        }

        public Builder code(TenantCode code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder status(TenantStatus status) {
            this.status = status != null ? status : TenantStatus.ACTIVE;
            return this;
        }

        public Builder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public Tenant build() {
            if (id == null) {
                throw new DomainException(ERROR_ID_REQUIRED);
            }
            if (code == null) {
                throw new DomainException(ERROR_CODE_REQUIRED);
            }
            if (name == null || name.isBlank()) {
                throw new DomainException(ERROR_NAME_REQUIRED);
            }
            if (status == null) {
                throw new DomainException(ERROR_STATUS_REQUIRED);
            }

            return new Tenant(this);
        }
    }
}
