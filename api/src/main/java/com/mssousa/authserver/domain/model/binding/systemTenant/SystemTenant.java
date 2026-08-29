package com.mssousa.authserver.domain.model.binding.systemTenant;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;

/**
 * Entidade de domínio representando o vínculo entre um Sistema e um Tenant.
 * <p>
 * Um sistema pertence a exatamente 1 tenant (decisão D3 do plano — cardinalidade
 * 1 sistema : 1 tenant, garantida no banco por {@code UNIQUE (system_id)}).
 * </p>
 */
public class SystemTenant {

    public static final String ERROR_ID_REQUIRED = "SystemTenantId é obrigatório no vínculo SystemTenant";
    public static final String ERROR_TENANT_ID_REQUIRED = "TenantId é obrigatório no vínculo SystemTenant";
    public static final String ERROR_SYSTEM_ID_REQUIRED = "SystemId é obrigatório no vínculo SystemTenant";
    public static final String ERROR_STATUS_REQUIRED = "Status do vínculo SystemTenant não pode ser nulo";
    public static final String ERROR_INACTIVE_BINDING = "Vínculo do sistema com o tenant está inativo ou bloqueado";

    private final SystemTenantId id;
    private final TenantId tenantId;
    private final SystemId systemId;
    private BindingStatus status;

    private SystemTenant(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.systemId = builder.systemId;
        this.status = builder.status;

        validate();
    }

    private void validate() {
        if (id == null) {
            throw new DomainException(ERROR_ID_REQUIRED);
        }
        if (tenantId == null) {
            throw new DomainException(ERROR_TENANT_ID_REQUIRED);
        }
        if (systemId == null) {
            throw new DomainException(ERROR_SYSTEM_ID_REQUIRED);
        }
        if (status == null) {
            throw new DomainException(ERROR_STATUS_REQUIRED);
        }
    }

    public SystemTenantId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public SystemId getSystemId() {
        return systemId;
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
        private SystemTenantId id;
        private TenantId tenantId;
        private SystemId systemId;
        private BindingStatus status = BindingStatus.ACTIVE;

        public Builder id(SystemTenantId id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder systemId(SystemId systemId) {
            this.systemId = systemId;
            return this;
        }

        public Builder status(BindingStatus status) {
            this.status = status != null ? status : BindingStatus.ACTIVE;
            return this;
        }

        public SystemTenant build() {
            if (id == null) {
                throw new DomainException(ERROR_ID_REQUIRED);
            }
            if (tenantId == null) {
                throw new DomainException(ERROR_TENANT_ID_REQUIRED);
            }
            if (systemId == null) {
                throw new DomainException(ERROR_SYSTEM_ID_REQUIRED);
            }
            if (status == null) {
                throw new DomainException(ERROR_STATUS_REQUIRED);
            }

            return new SystemTenant(this);
        }
    }
}
