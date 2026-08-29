package com.mssousa.authserver.adapter.in.web.admin;

import com.mssousa.authserver.adapter.in.web.common.UpdateStatusRequest;
import com.mssousa.authserver.application.port.in.ManageUserUseCase;
import com.mssousa.authserver.application.port.in.ResetPasswordUseCase;
import com.mssousa.authserver.domain.exception.DomainException;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.UserStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

/**
 * {@code /admin/api/v1/tenants/{tenantId}/users} (seção 9 do plano). Assim como
 * Sistemas/Perfis, {@code PUT}/{@code PATCH}/{@code POST reset-password} ficam aninhados
 * sob {@code tenantId} — {@code ManageUserUseCase} exige os dois IDs juntos (usuário é
 * escopado ao tenant, seção 3.2).
 */
@RestController
@RequestMapping("/admin/api/v1/tenants/{tenantId}/users")
public class UserController {

    private final ManageUserUseCase manageUserUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public UserController(ManageUserUseCase manageUserUseCase, ResetPasswordUseCase resetPasswordUseCase) {
        this.manageUserUseCase = manageUserUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@PathVariable Long tenantId, @Valid @RequestBody CreateUserRequest request) {
        User user = manageUserUseCase.createUser(TenantId.of(tenantId), request.username(), request.email(),
                request.password(), request.name());
        return UserResponse.from(user);
    }

    @GetMapping
    public Page<UserResponse> list(@PathVariable Long tenantId, Pageable pageable) {
        return manageUserUseCase.listUsersByTenant(TenantId.of(tenantId), pageable).map(UserResponse::from);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long tenantId, @PathVariable Long id) {
        return UserResponse.from(manageUserUseCase.getUser(TenantId.of(tenantId), UserId.of(id)));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long tenantId, @PathVariable Long id,
                                @Valid @RequestBody UpdateUserRequest request) {
        User user = manageUserUseCase.updateUser(TenantId.of(tenantId), UserId.of(id), request.name(), request.email());
        return UserResponse.from(user);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable Long tenantId, @PathVariable Long id,
                                      @Valid @RequestBody UpdateStatusRequest request) {
        UserStatus status = parseStatus(request.status());
        User user = switch (status) {
            case ACTIVE -> manageUserUseCase.activateUser(TenantId.of(tenantId), UserId.of(id));
            case BLOCKED -> manageUserUseCase.blockUser(TenantId.of(tenantId), UserId.of(id));
            case DISABLED -> manageUserUseCase.disableUser(TenantId.of(tenantId), UserId.of(id));
        };
        return UserResponse.from(user);
    }

    @PostMapping("/{id}/reset-password")
    public void resetPassword(@PathVariable Long tenantId, @PathVariable Long id) {
        resetPasswordUseCase.requestResetForUser(TenantId.of(tenantId), UserId.of(id));
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Status inválido para usuário: '" + status + "'");
        }
    }
}
