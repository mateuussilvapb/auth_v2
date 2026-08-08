package com.mssousa.authserver.domain.model.binding.userSystemProfile;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;

/**
 * Entidade de domínio representando o vínculo entre um {@code UserSystem} e um
 * {@code SystemProfile} — o perfil concedido a um usuário dentro de um sistema.
 * <p>
 * O perfil deve pertencer ao mesmo sistema do {@code UserSystem} (seção 3.2); essa
 * consistência é responsabilidade da camada de aplicação ao montar o vínculo, não desta
 * entidade isoladamente.
 * </p>
 */
public class UserSystemProfile {

    public static final String ERROR_ID_REQUIRED = "UserSystemProfileId é obrigatório no vínculo UserSystemProfile";
    public static final String ERROR_USER_SYSTEM_ID_REQUIRED = "UserSystemId é obrigatório no vínculo UserSystemProfile";
    public static final String ERROR_SYSTEM_PROFILE_ID_REQUIRED = "SystemProfileId é obrigatório no vínculo UserSystemProfile";
    public static final String ERROR_STATUS_REQUIRED = "Status do vínculo UserSystemProfile não pode ser nulo";
    public static final String ERROR_INACTIVE_BINDING = "Perfil não está ativo para este usuário no sistema";

    private final UserSystemProfileId id;
    private final UserSystemId userSystemId;
    private final SystemProfileId systemProfileId;
    private BindingStatus status;

    private UserSystemProfile(Builder builder) {
        this.id = builder.id;
        this.userSystemId = builder.userSystemId;
        this.systemProfileId = builder.systemProfileId;
        this.status = builder.status;

        validate();
    }

    private void validate() {
        if (id == null) {
            throw new DomainException(ERROR_ID_REQUIRED);
        }
        if (userSystemId == null) {
            throw new DomainException(ERROR_USER_SYSTEM_ID_REQUIRED);
        }
        if (systemProfileId == null) {
            throw new DomainException(ERROR_SYSTEM_PROFILE_ID_REQUIRED);
        }
        if (status == null) {
            throw new DomainException(ERROR_STATUS_REQUIRED);
        }
    }

    public UserSystemProfileId getId() {
        return id;
    }

    public UserSystemId getUserSystemId() {
        return userSystemId;
    }

    public SystemProfileId getSystemProfileId() {
        return systemProfileId;
    }

    public BindingStatus getStatus() {
        return status;
    }

    /**
     * Valida se o vínculo está ativo, lançando exceção caso contrário.
     */
    public void validateAccess() {
        if (!isActive()) {
            throw new DomainException(ERROR_INACTIVE_BINDING);
        }
    }

    public void activate() {
        this.status = BindingStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = BindingStatus.INACTIVE;
    }

    public void block() {
        this.status = BindingStatus.BLOCKED;
    }

    public boolean isActive() {
        return this.status == BindingStatus.ACTIVE;
    }

    public boolean isInactive() {
        return this.status == BindingStatus.INACTIVE;
    }

    public boolean isBlocked() {
        return this.status == BindingStatus.BLOCKED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UserSystemProfileId id;
        private UserSystemId userSystemId;
        private SystemProfileId systemProfileId;
        private BindingStatus status = BindingStatus.ACTIVE;

        public Builder id(UserSystemProfileId id) {
            this.id = id;
            return this;
        }

        public Builder userSystemId(UserSystemId userSystemId) {
            this.userSystemId = userSystemId;
            return this;
        }

        public Builder systemProfileId(SystemProfileId systemProfileId) {
            this.systemProfileId = systemProfileId;
            return this;
        }

        public Builder status(BindingStatus status) {
            this.status = status != null ? status : BindingStatus.ACTIVE;
            return this;
        }

        public UserSystemProfile build() {
            if (id == null) {
                throw new DomainException(ERROR_ID_REQUIRED);
            }
            if (userSystemId == null) {
                throw new DomainException(ERROR_USER_SYSTEM_ID_REQUIRED);
            }
            if (systemProfileId == null) {
                throw new DomainException(ERROR_SYSTEM_PROFILE_ID_REQUIRED);
            }
            if (status == null) {
                throw new DomainException(ERROR_STATUS_REQUIRED);
            }

            return new UserSystemProfile(this);
        }
    }
}
