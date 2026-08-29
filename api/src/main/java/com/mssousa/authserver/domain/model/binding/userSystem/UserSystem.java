package com.mssousa.authserver.domain.model.binding.userSystem;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;

/**
 * Entidade de domínio representando o vínculo entre um Usuário e um Sistema.
 * <p>
 * O {@code tenantId} é redundante de propósito (seção 4.4 do plano): existe apenas para
 * viabilizar as FKs compostas do banco que impedem, mesmo via SQL direto, um vínculo
 * cruzando tenants. A consistência entre {@code tenantId} aqui e o tenant real do usuário
 * e do sistema é responsabilidade do {@code TenantConsistencyValidator}, não desta classe.
 * </p>
 */
public class UserSystem {

    public static final String ERROR_ID_REQUIRED = "UserSystemId é obrigatório no vínculo UserSystem";
    public static final String ERROR_USER_ID_REQUIRED = "UserId é obrigatório no vínculo UserSystem";
    public static final String ERROR_SYSTEM_ID_REQUIRED = "SystemId é obrigatório no vínculo UserSystem";
    public static final String ERROR_TENANT_ID_REQUIRED = "TenantId é obrigatório no vínculo UserSystem";
    public static final String ERROR_STATUS_REQUIRED = "Status do vínculo UserSystem não pode ser nulo";
    public static final String ERROR_INACTIVE_BINDING = "Usuário não possui acesso ativo ao sistema";

    private final UserSystemId id;
    private final UserId userId;
    private final SystemId systemId;
    private final TenantId tenantId;
    private BindingStatus status;

    private UserSystem(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.systemId = builder.systemId;
        this.tenantId = builder.tenantId;
        this.status = builder.status;

        validate();
    }

    private void validate() {
        if (id == null) {
            throw new DomainException(ERROR_ID_REQUIRED);
        }
        if (userId == null) {
            throw new DomainException(ERROR_USER_ID_REQUIRED);
        }
        if (systemId == null) {
            throw new DomainException(ERROR_SYSTEM_ID_REQUIRED);
        }
        if (tenantId == null) {
            throw new DomainException(ERROR_TENANT_ID_REQUIRED);
        }
        if (status == null) {
            throw new DomainException(ERROR_STATUS_REQUIRED);
        }
    }

    public UserSystemId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public SystemId getSystemId() {
        return systemId;
    }

    public TenantId getTenantId() {
        return tenantId;
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
        private UserSystemId id;
        private UserId userId;
        private SystemId systemId;
        private TenantId tenantId;
        private BindingStatus status = BindingStatus.ACTIVE;

        public Builder id(UserSystemId id) {
            this.id = id;
            return this;
        }

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder systemId(SystemId systemId) {
            this.systemId = systemId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder status(BindingStatus status) {
            this.status = status != null ? status : BindingStatus.ACTIVE;
            return this;
        }

        public UserSystem build() {
            if (id == null) {
                throw new DomainException(ERROR_ID_REQUIRED);
            }
            if (userId == null) {
                throw new DomainException(ERROR_USER_ID_REQUIRED);
            }
            if (systemId == null) {
                throw new DomainException(ERROR_SYSTEM_ID_REQUIRED);
            }
            if (tenantId == null) {
                throw new DomainException(ERROR_TENANT_ID_REQUIRED);
            }
            if (status == null) {
                throw new DomainException(ERROR_STATUS_REQUIRED);
            }

            return new UserSystem(this);
        }
    }
}
