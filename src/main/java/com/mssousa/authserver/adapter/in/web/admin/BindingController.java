package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.adapter.in.web.common.UpdateStatusRequest;
import com.mssousa.authserver.application.port.in.ManageBindingUseCase;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.binding.BindingStatus;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystem;
import com.mssousa.authserver.domain.model.binding.userSystem.UserSystemId;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfile;
import com.mssousa.authserver.domain.model.binding.userSystemProfile.UserSystemProfileId;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vínculos usuário-sistema e usuário-sistema-perfil (seção 9 do plano). A tabela da seção
 * 9 mostra rotas como {@code /user-systems/{id}} sem tenant no caminho, mas
 * {@code ManageBindingUseCase} exige {@code tenantId} em toda operação (isolamento
 * multi-tenant é a prioridade #1 do projeto, seção 1.2/8) — todas as rotas ficam
 * aninhadas sob {@code /tenants/{tenantId}/...}.
 */
@RestController
@RequestMapping("/admin/api/v1/tenants/{tenantId}")
public class BindingController {

    private final ManageBindingUseCase manageBindingUseCase;

    public BindingController(ManageBindingUseCase manageBindingUseCase) {
        this.manageBindingUseCase = manageBindingUseCase;
    }

    @PostMapping("/users/{userId}/systems")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSystemResponse bindUserToSystem(@PathVariable Long tenantId, @PathVariable Long userId,
                                                @Valid @RequestBody BindSystemRequest request) {
        UserSystem binding = manageBindingUseCase.bindUserToSystem(
                TenantId.of(tenantId), UserId.of(userId), SystemId.of(request.systemId()));
        return UserSystemResponse.from(binding);
    }

    @PatchMapping("/user-systems/{id}/status")
    public UserSystemResponse updateUserSystemStatus(@PathVariable Long tenantId, @PathVariable Long id,
                                                       @Valid @RequestBody UpdateStatusRequest request) {
        BindingStatus status = parseStatus(request.status());
        UserSystem binding = switch (status) {
            case ACTIVE -> manageBindingUseCase.activateUserSystem(TenantId.of(tenantId), UserSystemId.of(id));
            case INACTIVE -> manageBindingUseCase.deactivateUserSystem(TenantId.of(tenantId), UserSystemId.of(id));
            case BLOCKED -> manageBindingUseCase.blockUserSystem(TenantId.of(tenantId), UserSystemId.of(id));
        };
        return UserSystemResponse.from(binding);
    }

    @PostMapping("/user-systems/{userSystemId}/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSystemProfileResponse bindProfile(@PathVariable Long tenantId, @PathVariable Long userSystemId,
                                                  @Valid @RequestBody BindProfileRequest request) {
        UserSystemProfile binding = manageBindingUseCase.bindProfileToUserSystem(
                TenantId.of(tenantId), UserSystemId.of(userSystemId), SystemProfileId.of(request.profileId()));
        return UserSystemProfileResponse.from(binding);
    }

    @PatchMapping("/user-systems/{userSystemId}/profiles/{id}/status")
    public UserSystemProfileResponse updateUserSystemProfileStatus(
            @PathVariable Long tenantId, @PathVariable Long userSystemId, @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        BindingStatus status = parseStatus(request.status());
        UserSystemProfileId profileBindingId = UserSystemProfileId.of(id);
        TenantId tid = TenantId.of(tenantId);
        UserSystemId usid = UserSystemId.of(userSystemId);
        UserSystemProfile binding = switch (status) {
            case ACTIVE -> manageBindingUseCase.activateUserSystemProfile(tid, usid, profileBindingId);
            case INACTIVE -> manageBindingUseCase.deactivateUserSystemProfile(tid, usid, profileBindingId);
            case BLOCKED -> manageBindingUseCase.blockUserSystemProfile(tid, usid, profileBindingId);
        };
        return UserSystemProfileResponse.from(binding);
    }

    private BindingStatus parseStatus(String status) {
        try {
            return BindingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Status inválido para vínculo: '" + status + "'");
        }
    }
}
