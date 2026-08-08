package com.mssousa.authserver.domain.model.profile;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.system.SystemId;

/**
 * Entidade de domínio representando um Perfil dentro de um Sistema.
 * <p>
 * O token carrega apenas o código do perfil — nenhuma permissão, menu ou ação (seção 1.2
 * do plano). Único por sistema ({@code UNIQUE (systemId, code)}), repetível entre sistemas.
 * </p>
 */
public class SystemProfile {

    public static final String ERROR_ID_REQUIRED = "ID do perfil não pode ser nulo";
    public static final String ERROR_SYSTEM_ID_REQUIRED = "SystemId do perfil não pode ser nulo";
    public static final String ERROR_STATUS_REQUIRED = "Status do perfil não pode ser nulo";

    private final SystemProfileId id;
    private final SystemId systemId;
    private final ProfileCode code;
    private String description;
    private ProfileStatus status;

    private SystemProfile(Builder builder) {
        this.id = builder.id;
        this.systemId = builder.systemId;
        this.code = builder.code;
        this.description = builder.description;
        this.status = builder.status;

        validate();
    }

    private void validate() {
        if (id == null) {
            throw new DomainException(ERROR_ID_REQUIRED);
        }
        if (systemId == null) {
            throw new DomainException(ERROR_SYSTEM_ID_REQUIRED);
        }
        if (code == null) {
            throw new DomainException(ProfileCode.ERROR_REQUIRED);
        }
        if (status == null) {
            throw new DomainException(ERROR_STATUS_REQUIRED);
        }
    }

    public SystemProfileId getId() {
        return id;
    }

    public SystemId getSystemId() {
        return systemId;
    }

    public ProfileCode getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public ProfileStatus getStatus() {
        return status;
    }

    /**
     * Ativa o perfil, permitindo que ele seja atribuído a usuários. Operação idempotente.
     */
    public void activate() {
        this.status = ProfileStatus.ACTIVE;
    }

    /**
     * Desativa o perfil, impedindo que ele seja atribuído a usuários. Operação idempotente.
     */
    public void deactivate() {
        this.status = ProfileStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == ProfileStatus.ACTIVE;
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }

    /**
     * Verifica se o perfil pertence ao sistema informado.
     */
    public boolean belongsTo(SystemId systemId) {
        return this.systemId.equals(systemId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SystemProfileId id;
        private SystemId systemId;
        private ProfileCode code;
        private String description;
        private ProfileStatus status = ProfileStatus.ACTIVE;

        public Builder id(SystemProfileId id) {
            this.id = id;
            return this;
        }

        public Builder systemId(SystemId systemId) {
            this.systemId = systemId;
            return this;
        }

        public Builder code(ProfileCode code) {
            this.code = code;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(ProfileStatus status) {
            this.status = status != null ? status : ProfileStatus.ACTIVE;
            return this;
        }

        public SystemProfile build() {
            if (id == null) {
                throw new DomainException(SystemProfile.ERROR_ID_REQUIRED);
            }
            if (systemId == null) {
                throw new DomainException(SystemProfile.ERROR_SYSTEM_ID_REQUIRED);
            }
            if (code == null) {
                throw new DomainException(ProfileCode.ERROR_REQUIRED);
            }
            if (status == null) {
                throw new DomainException(SystemProfile.ERROR_STATUS_REQUIRED);
            }

            return new SystemProfile(this);
        }
    }
}
