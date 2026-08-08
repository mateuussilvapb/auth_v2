package com.mssousa.authserver.adapter.out.persistence.mapper;

import com.mssousa.authserver.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.PlatformAdminEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.SystemEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.SystemProfileEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.SystemTenantEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.TenantEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.UserEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.UserSystemEntity;
import com.mssousa.authserver.adapter.out.persistence.entity.UserSystemProfileEntity;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import com.mssousa.authserver.domain.model.profile.ProfileCode;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.ClientId;
import com.mssousa.authserver.domain.model.system.RedirectUri;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.tenant.TenantCode;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetToken;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetTokenId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.Password;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AuthMapperTest {

    private final AtomicLong sequence = new AtomicLong(1000L);
    private final AuthMapper mapper = new AuthMapper(sequence::incrementAndGet);

    @Test
    void deveConverterTenantEntreDominioEEntidade() {
        Tenant tenant = Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build();

        TenantEntity entity = mapper.toEntity(tenant);
        Tenant reconstruido = mapper.toDomain(entity);

        assertEquals(1L, entity.getId());
        assertEquals("acme", entity.getCode());
        assertEquals(tenant.getCode(), reconstruido.getCode());
        assertEquals(tenant.getStatus(), reconstruido.getStatus());
    }

    @Test
    void deveRetornarNuloParaEntidadesNulas() {
        assertNull(mapper.toDomain((TenantEntity) null));
        assertNull(mapper.toEntity((Tenant) null));
    }

    @Test
    void deveConverterPlatformAdminEntreDominioEEntidade() {
        PlatformAdmin admin = PlatformAdmin.builder()
                .id(PlatformAdminId.of(1L))
                .username(Username.of("root_admin"))
                .email(Email.of("admin@seudominio.com"))
                .password(Password.fromPlainText("senhaSegura123"))
                .name("Administrador")
                .build();

        PlatformAdminEntity entity = mapper.toEntity(admin);
        PlatformAdmin reconstruido = mapper.toDomain(entity);

        assertEquals(admin.getUsername(), reconstruido.getUsername());
        assertEquals(admin.getEmail(), reconstruido.getEmail());
        assertTrue(reconstruido.verifyPassword("senhaSegura123"));
    }

    @Test
    void deveConverterSystemComRedirectUrisEntreDominioEEntidade() {
        System system = System.builder()
                .id(SystemId.of(1L))
                .clientId(ClientId.of("CRM_ACME"))
                .name("CRM Acme")
                .publicClient(true)
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback"))
                .redirectUri(RedirectUri.of("https://crm.acme.com/dev-callback"))
                .build();

        SystemEntity entity = mapper.toEntity(system);

        assertEquals(2, entity.getRedirectUris().size());
        entity.getRedirectUris().forEach(uriEntity -> {
            assertNotNull(uriEntity.getId());
            assertSame(entity, uriEntity.getSystem());
        });

        System reconstruido = mapper.toDomain(entity);
        assertEquals(system.getClientId(), reconstruido.getClientId());
        assertEquals(2, reconstruido.getRedirectUris().size());
        assertTrue(reconstruido.matchesRedirectUri("https://crm.acme.com/callback"));
        assertTrue(reconstruido.matchesRedirectUri("https://crm.acme.com/dev-callback"));
    }

    @Test
    void deveConverterSystemTenantEntreDominioEEntidade() {
        SystemTenant systemTenant = SystemTenant.builder()
                .id(SystemTenantId.of(1L)).tenantId(TenantId.of(1L)).systemId(SystemId.of(1L)).build();

        TenantEntity tenantEntity = mapper.toEntity(Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build());
        SystemEntity systemEntity = mapper.toEntity(System.builder()
                .id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback")).build());

        SystemTenantEntity entity = mapper.toEntity(systemTenant, tenantEntity, systemEntity);
        SystemTenant reconstruido = mapper.toDomain(entity);

        assertEquals(systemTenant.getTenantId(), reconstruido.getTenantId());
        assertEquals(systemTenant.getSystemId(), reconstruido.getSystemId());
        assertEquals(BindingStatus.ACTIVE, reconstruido.getStatus());
    }

    @Test
    void deveConverterUserEntreDominioEEntidade() {
        User user = User.builder()
                .id(UserId.of(1L)).tenantId(TenantId.of(1L))
                .username(Username.of("joao_silva")).email(Email.of("joao@acme.com"))
                .password(Password.fromPlainText("senhaSegura123")).name("João da Silva").build();

        TenantEntity tenantEntity = mapper.toEntity(Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build());
        UserEntity entity = mapper.toEntity(user, tenantEntity);
        User reconstruido = mapper.toDomain(entity);

        assertEquals(user.getUsername(), reconstruido.getUsername());
        assertEquals(user.getTenantId(), reconstruido.getTenantId());
        assertTrue(reconstruido.verifyPassword("senhaSegura123"));
    }

    @Test
    void deveConverterSystemProfileEntreDominioEEntidade() {
        SystemProfile profile = SystemProfile.builder()
                .id(SystemProfileId.of(1L)).systemId(SystemId.of(1L)).code(ProfileCode.of("ADMIN"))
                .description("Administrador").build();

        SystemEntity systemEntity = mapper.toEntity(System.builder()
                .id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback")).build());

        SystemProfileEntity entity = mapper.toEntity(profile, systemEntity);
        SystemProfile reconstruido = mapper.toDomain(entity);

        assertEquals(profile.getCode(), reconstruido.getCode());
        assertEquals(profile.getSystemId(), reconstruido.getSystemId());
    }

    @Test
    void deveConverterUserSystemEntreDominioEEntidade() {
        UserSystem userSystem = UserSystem.builder()
                .id(UserSystemId.of(1L)).userId(UserId.of(1L)).systemId(SystemId.of(1L)).tenantId(TenantId.of(1L)).build();

        TenantEntity tenantEntity = mapper.toEntity(Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build());
        UserEntity userEntity = mapper.toEntity(User.builder()
                .id(UserId.of(1L)).tenantId(TenantId.of(1L)).username(Username.of("joao_silva"))
                .email(Email.of("joao@acme.com")).password(Password.fromPlainText("senhaSegura123"))
                .name("João").build(), tenantEntity);
        SystemEntity systemEntity = mapper.toEntity(System.builder()
                .id(SystemId.of(1L)).clientId(ClientId.of("CRM_ACME")).name("CRM")
                .redirectUri(RedirectUri.of("https://crm.acme.com/callback")).build());

        UserSystemEntity entity = mapper.toEntity(userSystem, userEntity, systemEntity);
        UserSystem reconstruido = mapper.toDomain(entity);

        assertEquals(userSystem.getUserId(), reconstruido.getUserId());
        assertEquals(userSystem.getSystemId(), reconstruido.getSystemId());
        assertEquals(userSystem.getTenantId(), reconstruido.getTenantId());
    }

    @Test
    void deveConverterUserSystemProfileEntreDominioEEntidade() {
        UserSystemProfile userSystemProfile = UserSystemProfile.builder()
                .id(UserSystemProfileId.of(1L)).userSystemId(UserSystemId.of(1L)).systemProfileId(SystemProfileId.of(1L)).build();

        UserSystemEntity userSystemEntity = new UserSystemEntity();
        userSystemEntity.setId(1L);
        SystemProfileEntity systemProfileEntity = new SystemProfileEntity();
        systemProfileEntity.setId(1L);

        UserSystemProfileEntity entity = mapper.toEntity(userSystemProfile, userSystemEntity, systemProfileEntity);
        UserSystemProfile reconstruido = mapper.toDomain(entity);

        assertEquals(userSystemProfile.getUserSystemId(), reconstruido.getUserSystemId());
        assertEquals(userSystemProfile.getSystemProfileId(), reconstruido.getSystemProfileId());
    }

    @Test
    void deveConverterPasswordResetTokenEntreDominioEEntidadePreservandoInstante() {
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        PasswordResetToken.GeneratedToken generated = PasswordResetToken.create(
                PasswordResetTokenId.of(1L), UserId.of(1L), expiresAt);

        TenantEntity tenantEntity = mapper.toEntity(Tenant.builder().id(TenantId.of(1L)).code(TenantCode.of("acme")).name("Acme").build());
        UserEntity userEntity = mapper.toEntity(User.builder()
                .id(UserId.of(1L)).tenantId(TenantId.of(1L)).username(Username.of("joao_silva"))
                .email(Email.of("joao@acme.com")).password(Password.fromPlainText("senhaSegura123"))
                .name("João").build(), tenantEntity);

        PasswordResetTokenEntity entity = mapper.toEntity(generated.token(), userEntity);
        PasswordResetToken reconstruido = mapper.toDomain(entity);

        assertEquals(generated.token().getValue(), reconstruido.getValue());
        assertEquals(expiresAt, reconstruido.getExpiresAt());
        assertFalse(reconstruido.isUsed());
    }
}
