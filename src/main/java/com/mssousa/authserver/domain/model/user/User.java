package com.mssousa.authserver.domain.model.user;

import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.tenant.TenantId;

/**
 * Entidade de domínio representando um Usuário final.
 * <p>
 * Pertence a exatamente um tenant ({@code tenantId} nunca nulo — ver decisão D2 do plano).
 * Diferente do projeto de referência, não existe usuário "master": o usuário que opera
 * acima dos tenants é o {@code PlatformAdmin}, uma entidade separada.
 * </p>
 */
public class User {

    public static final String ERROR_ID_REQUIRED = "Id não pode ser nulo";
    public static final String ERROR_TENANT_ID_REQUIRED = "TenantId não pode ser nulo";
    public static final String ERROR_NAME_REQUIRED = "Nome do usuário não pode ser nulo ou vazio";

    private final UserId id;
    private final TenantId tenantId;
    private final Username username;
    private Email email;
    private Password password;
    private String name;
    private UserStatus status;

    private User(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
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
        if (tenantId == null) {
            throw new DomainException(ERROR_TENANT_ID_REQUIRED);
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

    public UserId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
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

    public UserStatus getStatus() {
        return status;
    }

    /**
     * Ativa o usuário. Operação idempotente.
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Bloqueia o usuário (violação de segurança/política). Operação idempotente.
     */
    public void block() {
        this.status = UserStatus.BLOCKED;
    }

    /**
     * Desabilita o usuário (suspensão temporária). Operação idempotente.
     */
    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return this.status == UserStatus.BLOCKED;
    }

    public boolean isDisabled() {
        return this.status == UserStatus.DISABLED;
    }

    /**
     * Atualiza a senha do usuário.
     *
     * @param newPassword nova senha (já em formato hash)
     * @throws DomainException se a nova senha for nula
     */
    public void changePassword(Password newPassword) {
        if (newPassword == null) {
            throw new DomainException(Password.DEFAULT_ERROR_PASSWORD);
        }
        this.password = newPassword;
    }

    /**
     * Verifica se a senha em texto plano corresponde à senha armazenada.
     */
    public boolean verifyPassword(String plainPassword) {
        return this.password.matches(plainPassword);
    }

    /**
     * Verifica se o usuário pode fazer login, considerando apenas o próprio estado.
     * <p>
     * Não avalia tenant, sistema ou vínculos — isso é responsabilidade do
     * {@code AccessValidator} (seção 3.4 do plano), que compõe a cascata completa.
     * </p>
     */
    public boolean canLogin() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Verifica se o usuário pertence ao tenant informado.
     */
    public boolean belongsToTenant(TenantId tenantId) {
        return this.tenantId.equals(tenantId);
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
        private UserId id;
        private TenantId tenantId;
        private Username username;
        private Email email;
        private Password password;
        private String name;
        private UserStatus status = UserStatus.ACTIVE;

        public Builder id(UserId id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
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

        public Builder status(UserStatus status) {
            this.status = status != null ? status : UserStatus.ACTIVE;
            return this;
        }

        public User build() {
            if (id == null) {
                throw new DomainException(User.ERROR_ID_REQUIRED);
            }
            if (tenantId == null) {
                throw new DomainException(User.ERROR_TENANT_ID_REQUIRED);
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
                throw new DomainException(User.ERROR_NAME_REQUIRED);
            }

            return new User(this);
        }
    }
}
