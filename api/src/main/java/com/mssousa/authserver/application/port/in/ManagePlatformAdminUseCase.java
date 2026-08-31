package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Porta de entrada para administração de platform admins (seção 9:
 * {@code /admin/api/v1/platform-admins}). Diferente dos demais recursos administrativos,
 * a Fase 4 deliberadamente não cobriu este caso — "gestão de tenant, sistema, perfil,
 * usuário e vínculos" (seção 10) não inclui platform admin — então a implementação vem
 * junto da Fase 8, onde a API que o expõe é construída.
 */
public interface ManagePlatformAdminUseCase {

    PlatformAdmin createPlatformAdmin(String username, String email, String plainPassword, String name);

    PlatformAdmin activatePlatformAdmin(PlatformAdminId id);

    /**
     * @throws com.mssousa.authserver.domain.exception.DomainException se {@code id} for o
     *                                                                  último platform admin ativo ({@code PlatformAdminPolicy}, seção 2.1)
     */
    PlatformAdmin deactivatePlatformAdmin(PlatformAdminId id);

    Page<PlatformAdmin> listPlatformAdmins(Pageable pageable);

    /**
     * Troca a própria senha (self-service) — único jeito de sair do estado
     * {@code mustChangePassword=true} (seção 10, Fase 10: seed inicial com senha
     * temporária forçando troca).
     *
     * @throws com.mssousa.authserver.domain.exception.DomainException se {@code currentPassword}
     *                                                                  não bater com a senha atual
     */
    PlatformAdmin changeOwnPassword(PlatformAdminId id, String currentPassword, String newPassword);
}
