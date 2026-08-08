package com.mssousa.authserver.domain.model.platform;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.Username;

/**
 * Entidade de domínio representando o Platform Admin ("usuário deus").
 * <p>
 * Opera acima de todos os tenants. Fica em tabela separada de {@code User} para manter
 * a invariante {@code user.tenant_id IS NOT NULL} absoluta — ver decisão D9 e seção 2.1
 * do plano. Reaproveita os VOs de credenciais de {@code User} (mesma forma, agregado
 * diferente).
 * </p>
 */
public class PlatformAdmin {

    public static final String ERROR_ID_REQUIRED = "Id não pode ser nulo";
    public static final String ERROR_NAME_REQUIRED = "Nome do platform admin não pode ser nulo ou vazio";

    private final PlatformAdminId id;
    private final Username username;
    private Email email;
    private Password password;
    private String name;
    private PlatformAdminStatus status;

    private PlatformAdmin(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.name = builder.name;
        this.status = builder.status;

        validate();
    }

    private void validate() {
        if (id == null) {
            throw new DomainException(ERROR_ID_REQUIRED);
        }
        if (username == null) {
            throw new DomainException(Username.DEFAULT_ERROR_USERNAME);
        }
        if (email == null) {
            throw new DomainException(Email.DEFAULT_ERROR_EMAIL);
        }
        if (password == null) {
            throw new DomainException(Password.DEFAULT_ERROR_PASSWORD);
        }
        if (name == null || name.isBlank()) {
            throw new DomainException(ERROR_NAME_REQUIRED);
        }
    }

    public PlatformAdminId getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public PlatformAdminStatus getStatus() {
        return status;
    }

    /**
     * Ativa o platform admin. Operação idempotente.
     */
    public void activate() {
        this.status = PlatformAdminStatus.ACTIVE;
    }

    /**
     * Desativa o platform admin. Operação idempotente.
     */
    public void deactivate() {
        this.status = PlatformAdminStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == PlatformAdminStatus.ACTIVE;
    }

    public boolean canLogin() {
        return status == PlatformAdminStatus.ACTIVE;
    }

    public void changePassword(Password newPassword) {
        if (newPassword == null) {
            throw new DomainException(Password.DEFAULT_ERROR_PASSWORD);
        }
        this.password = newPassword;
    }

    public boolean verifyPassword(String plainPassword) {
        return this.password.matches(plainPassword);
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException(ERROR_NAME_REQUIRED);
        }
        this.name = name;
    }

    public void updateEmail(Email email) {
        if (email == null) {
            throw new DomainException(Email.DEFAULT_ERROR_EMAIL);
        }
        this.email = email;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PlatformAdminId id;
        private Username username;
        private Email email;
        private Password password;
        private String name;
        private PlatformAdminStatus status = PlatformAdminStatus.ACTIVE;

        public Builder id(PlatformAdminId id) {
            this.id = id;
            return this;
        }

        public Builder username(Username username) {
            this.username = username;
            return this;
        }

        public Builder email(Email email) {
            this.email = email;
            return this;
        }

        public Builder password(Password password) {
            this.password = password;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder status(PlatformAdminStatus status) {
            this.status = status != null ? status : PlatformAdminStatus.ACTIVE;
            return this;
        }

        public PlatformAdmin build() {
            if (id == null) {
                throw new DomainException(PlatformAdmin.ERROR_ID_REQUIRED);
            }
            if (username == null) {
                throw new DomainException(Username.DEFAULT_ERROR_USERNAME);
            }
            if (email == null) {
                throw new DomainException(Email.DEFAULT_ERROR_EMAIL);
            }
            if (password == null) {
                throw new DomainException(Password.DEFAULT_ERROR_PASSWORD);
            }
            if (name == null || name.isBlank()) {
                throw new DomainException(PlatformAdmin.ERROR_NAME_REQUIRED);
            }

            return new PlatformAdmin(this);
        }
    }
}
