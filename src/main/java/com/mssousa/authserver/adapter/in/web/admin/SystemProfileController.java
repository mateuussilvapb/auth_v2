package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.adapter.in.web.common.UpdateStatusRequest;
import com.mssousa.authserver.application.port.in.ManageProfileUseCase;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.profile.ProfileStatus;
import com.mssousa.authserver.domain.model.profile.SystemProfile;
import com.mssousa.authserver.domain.model.profile.SystemProfileId;
import com.mssousa.authserver.domain.model.system.SystemId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code /admin/api/v1/systems/{systemId}/profiles} (seção 9 do plano). A seção 9 lista
 * {@code PUT /profiles/{id}} e {@code PATCH /profiles/{id}/status} sem o {@code systemId}
 * no caminho, mas {@code ManageProfileUseCase} exige os dois (perfil só existe dentro de
 * um sistema, {@code UNIQUE (systemId, code)}) — as rotas ficam aninhadas sob
 * {@code /systems/{systemId}/profiles/{id}} para não precisar de uma busca extra só para
 * descobrir o systemId a partir do id do perfil.
 */
@RestController
@RequestMapping("/admin/api/v1/systems/{systemId}/profiles")
public class SystemProfileController {

    private final ManageProfileUseCase manageProfileUseCase;

    public SystemProfileController(ManageProfileUseCase manageProfileUseCase) {
        this.manageProfileUseCase = manageProfileUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemProfileResponse create(@PathVariable Long systemId, @Valid @RequestBody CreateSystemProfileRequest request) {
        SystemProfile profile = manageProfileUseCase.createProfile(SystemId.of(systemId), request.code(), request.description());
        return SystemProfileResponse.from(profile);
    }

    @GetMapping
    public List<SystemProfileResponse> list(@PathVariable Long systemId) {
        return manageProfileUseCase.listProfilesBySystem(SystemId.of(systemId)).stream()
                .map(SystemProfileResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SystemProfileResponse get(@PathVariable Long systemId, @PathVariable Long id) {
        return SystemProfileResponse.from(manageProfileUseCase.getProfile(SystemId.of(systemId), SystemProfileId.of(id)));
    }

    @PutMapping("/{id}")
    public SystemProfileResponse update(@PathVariable Long systemId, @PathVariable Long id,
                                         @Valid @RequestBody UpdateSystemProfileRequest request) {
        SystemProfile profile = manageProfileUseCase.updateProfileDescription(
                SystemId.of(systemId), SystemProfileId.of(id), request.description());
        return SystemProfileResponse.from(profile);
    }

    @PatchMapping("/{id}/status")
    public SystemProfileResponse updateStatus(@PathVariable Long systemId, @PathVariable Long id,
                                               @Valid @RequestBody UpdateStatusRequest request) {
        ProfileStatus status = parseStatus(request.status());
        SystemProfile profile = status == ProfileStatus.ACTIVE
                ? manageProfileUseCase.activateProfile(SystemId.of(systemId), SystemProfileId.of(id))
                : manageProfileUseCase.deactivateProfile(SystemId.of(systemId), SystemProfileId.of(id));
        return SystemProfileResponse.from(profile);
    }

    private ProfileStatus parseStatus(String status) {
        try {
            return ProfileStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Status inválido para perfil: '" + status + "'");
        }
    }
}
