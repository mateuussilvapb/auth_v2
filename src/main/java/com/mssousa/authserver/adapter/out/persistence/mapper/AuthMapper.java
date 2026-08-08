package com.mssousa.authserver.adapter.out.persistence.mapper;

import com.mssousa.authserver.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.PlatformAdminEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.SystemEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.SystemProfileEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.SystemRedirectUriEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.SystemTenantEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.TenantEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.UserEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.UserSystemEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.UserSystemProfileEntity;
import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.platform.PlatformAdminStatus;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.ProfileStatus;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.system.SystemStatus;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.tenant.TenantStatus;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetToken;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetTokenId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.ResetTokenValue;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.UserStatus;
import com.mssousa.authserver.domain.model.user.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Converte entre agregados de domínio e entidades JPA. Nenhuma entidade JPA cruza a
 * fronteira de {@code adapter.out.persistence} (regra 5.1.4 do plano) — todo
 * {@code *RepositoryImpl} passa por aqui.
 * <p>
 * IDs de agregado são sempre gerados na aplicação antes de chegar aqui (seção 6.4); o
 * único caso em que este mapper gera ID é para as linhas filhas de
 * {@code system_redirect_uri}, porque {@link RedirectUri} é um Value Object sem
 * identidade no domínio — cada {@code toEntity(System)} produz linhas novas, e o
 * {@code cascade + orphanRemoval} do relacionamento cuida de substituir as antigas.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final IdGeneratorPort idGenerator;

    // ==================== Tenant ====================

    public Tenant toDomain(TenantEntity entity) {
        if (entity == null) return null;
        return Tenant.builder()
                .id(TenantId.of(entity.getId()))
                .code(TenantCode.of(entity.getCode()))
                .name(entity.getName())
                .status(TenantStatus.valueOf(entity.getStatus()))
                .build();
    }

    public TenantEntity toEntity(Tenant tenant) {
        if (tenant == null) return null;
        TenantEntity entity = new TenantEntity();
        entity.setId(tenant.getId().value());
        entity.setCode(tenant.getCode().value());
        entity.setName(tenant.getName());
        entity.setStatus(tenant.getStatus().name());
        return entity;
    }

    // ==================== PlatformAdmin ====================

    public PlatformAdmin toDomain(PlatformAdminEntity entity) {
        if (entity == null) return null;
        return PlatformAdmin.builder()
                .id(PlatformAdminId.of(entity.getId()))
                .username(Username.of(entity.getUsername()))
                .email(Email.of(entity.getEmail()))
                .password(Password.fromHash(entity.getPasswordHash()))
                .name(entity.getName())
                .status(PlatformAdminStatus.valueOf(entity.getStatus()))
                .build();
    }

    public PlatformAdminEntity toEntity(PlatformAdmin admin) {
        if (admin == null) return null;
        PlatformAdminEntity entity = new PlatformAdminEntity();
        entity.setId(admin.getId().value());
        entity.setUsername(admin.getUsername().value());
        entity.setEmail(admin.getEmail().value());
        entity.setPasswordHash(admin.getPassword().hashedValue());
        entity.setName(admin.getName());
        entity.setStatus(admin.getStatus().name());
        return entity;
    }

    // ==================== System (+ SystemRedirectUri) ====================

    public System toDomain(SystemEntity entity) {
        if (entity == null) return null;
        System.Builder builder = System.builder()
                .id(SystemId.of(entity.getId()))
                .clientId(ClientId.of(entity.getClientId()))
                .clientSecret(entity.getClientSecret())
                .name(entity.getName())
                .publicClient(entity.isPublicClient())
                .status(SystemStatus.valueOf(entity.getStatus()));

        entity.getRedirectUris().forEach(uriEntity -> builder.redirectUri(RedirectUri.of(uriEntity.getUri())));
        return builder.build();
    }

    public SystemEntity toEntity(System system) {
        if (system == null) return null;
        SystemEntity entity = new SystemEntity();
        entity.setId(system.getId().value());
        entity.setClientId(system.getClientId().value());
        entity.setClientSecret(system.getClientSecret());
        entity.setName(system.getName());
        entity.setPublicClient(system.isPublicClient());
        entity.setStatus(system.getStatus().name());

        List<SystemRedirectUriEntity> redirectUris = system.getRedirectUris().stream()
                .map(redirectUri -> toEntity(redirectUri, entity))
                .toList();
        entity.getRedirectUris().addAll(redirectUris);

        return entity;
    }

    private SystemRedirectUriEntity toEntity(RedirectUri redirectUri, SystemEntity system) {
        SystemRedirectUriEntity entity = new SystemRedirectUriEntity();
        entity.setId(idGenerator.generate());
        entity.setSystem(system);
        entity.setUri(redirectUri.value());
        return entity;
    }

    // ==================== SystemTenant ====================

    public SystemTenant toDomain(SystemTenantEntity entity) {
        if (entity == null) return null;
        return SystemTenant.builder()
                .id(SystemTenantId.of(entity.getId()))
                .tenantId(TenantId.of(entity.getTenant().getId()))
                .systemId(SystemId.of(entity.getSystem().getId()))
                .status(BindingStatus.valueOf(entity.getStatus()))
                .build();
    }

    public SystemTenantEntity toEntity(SystemTenant systemTenant, TenantEntity tenant, SystemEntity system) {
        if (systemTenant == null) return null;
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setId(systemTenant.getId().value());
        entity.setTenant(tenant);
        entity.setSystem(system);
        entity.setStatus(systemTenant.getStatus().name());
        return entity;
    }

    // ==================== User ====================

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(UserId.of(entity.getId()))
                .tenantId(TenantId.of(entity.getTenant().getId()))
                .username(Username.of(entity.getUsername()))
                .email(Email.of(entity.getEmail()))
                .password(Password.fromHash(entity.getPasswordHash()))
                .name(entity.getName())
                .status(UserStatus.valueOf(entity.getStatus()))
                .build();
    }

    public UserEntity toEntity(User user, TenantEntity tenant) {
        if (user == null) return null;
        UserEntity entity = new UserEntity();
        entity.setId(user.getId().value());
        entity.setTenant(tenant);
        entity.setUsername(user.getUsername().value());
        entity.setEmail(user.getEmail().value());
        entity.setPasswordHash(user.getPassword().hashedValue());
        entity.setName(user.getName());
        entity.setStatus(user.getStatus().name());
        return entity;
    }

    // ==================== SystemProfile ====================

    public SystemProfile toDomain(SystemProfileEntity entity) {
        if (entity == null) return null;
        return SystemProfile.builder()
                .id(SystemProfileId.of(entity.getId()))
                .systemId(SystemId.of(entity.getSystem().getId()))
                .code(ProfileCode.of(entity.getCode()))
                .description(entity.getDescription())
                .status(ProfileStatus.valueOf(entity.getStatus()))
                .build();
    }

    public SystemProfileEntity toEntity(SystemProfile profile, SystemEntity system) {
        if (profile == null) return null;
        SystemProfileEntity entity = new SystemProfileEntity();
        entity.setId(profile.getId().value());
        entity.setSystem(system);
        entity.setCode(profile.getCode().value());
        entity.setDescription(profile.getDescription());
        entity.setStatus(profile.getStatus().name());
        return entity;
    }

    // ==================== UserSystem ====================

    public UserSystem toDomain(UserSystemEntity entity) {
        if (entity == null) return null;
        return UserSystem.builder()
                .id(UserSystemId.of(entity.getId()))
                .userId(UserId.of(entity.getUser().getId()))
                .systemId(SystemId.of(entity.getSystem().getId()))
                .tenantId(TenantId.of(entity.getTenantId()))
                .status(BindingStatus.valueOf(entity.getStatus()))
                .build();
    }

    public UserSystemEntity toEntity(UserSystem userSystem, UserEntity user, SystemEntity system) {
        if (userSystem == null) return null;
        UserSystemEntity entity = new UserSystemEntity();
        entity.setId(userSystem.getId().value());
        entity.setUser(user);
        entity.setSystem(system);
        entity.setTenantId(userSystem.getTenantId().value());
        entity.setStatus(userSystem.getStatus().name());
        return entity;
    }

    // ==================== UserSystemProfile ====================

    public UserSystemProfile toDomain(UserSystemProfileEntity entity) {
        if (entity == null) return null;
        return UserSystemProfile.builder()
                .id(UserSystemProfileId.of(entity.getId()))
                .userSystemId(UserSystemId.of(entity.getUserSystem().getId()))
                .systemProfileId(SystemProfileId.of(entity.getSystemProfile().getId()))
                .status(BindingStatus.valueOf(entity.getStatus()))
                .build();
    }

    public UserSystemProfileEntity toEntity(UserSystemProfile userSystemProfile, UserSystemEntity userSystem, SystemProfileEntity systemProfile) {
        if (userSystemProfile == null) return null;
        UserSystemProfileEntity entity = new UserSystemProfileEntity();
        entity.setId(userSystemProfile.getId().value());
        entity.setUserSystem(userSystem);
        entity.setSystemProfile(systemProfile);
        entity.setStatus(userSystemProfile.getStatus().name());
        return entity;
    }

    // ==================== PasswordResetToken ====================

    public PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        if (entity == null) return null;
        return PasswordResetToken.builder()
                .id(PasswordResetTokenId.of(entity.getId()))
                .value(ResetTokenValue.ofHash(entity.getTokenHash()))
                .userId(UserId.of(entity.getUser().getId()))
                .expiresAt(entity.getExpiresAt().toInstant(ZoneOffset.UTC))
                .used(entity.isUsed())
                .build();
    }

    public PasswordResetTokenEntity toEntity(PasswordResetToken token, UserEntity user) {
        if (token == null) return null;
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setId(token.getId().value());
        entity.setTokenHash(token.getValue().value());
        entity.setUser(user);
        entity.setExpiresAt(token.getExpiresAt().atZone(ZoneOffset.UTC).toLocalDateTime());
        entity.setUsed(token.getUsed());
        return entity;
    }
}
