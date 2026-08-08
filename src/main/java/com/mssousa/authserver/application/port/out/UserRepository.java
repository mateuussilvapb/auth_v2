package com.mssousa.authserver.application.port.out;

import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.Email;
import com.mssousa.authserver.domain.model.user.User;
import com.mssousa.authserver.domain.model.user.UserId;
import com.mssousa.authserver.domain.model.user.Username;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Regra absoluta (seção 6.5 do plano): toda consulta recebe {@link TenantId} explícito
 * como primeiro parâmetro. Um {@code findByUsername(Username)} sem tenant seria um bug
 * de segurança, não uma conveniência.
 */
public interface UserRepository {

    User save(User user);

    /**
     * Busca por chave primária, sem escopo de tenant. Exceção deliberada à regra acima:
     * não é uma busca (não há como "vazar" outro tenant nela — o ID já identifica a
     * linha exata), usada apenas onde o tenant genuinamente não está disponível, como no
     * fluxo de confirmação de redefinição de senha (o token carrega só o {@code userId}).
     */
    Optional<User> findById(UserId id);

    Optional<User> findByTenantIdAndId(TenantId tenantId, UserId id);

    Optional<User> findByTenantIdAndUsername(TenantId tenantId, Username username);

    Optional<User> findByTenantIdAndEmail(TenantId tenantId, Email email);

    boolean existsByTenantIdAndUsername(TenantId tenantId, Username username);

    boolean existsByTenantIdAndEmail(TenantId tenantId, Email email);

    Page<User> findByTenantId(TenantId tenantId, Pageable pageable);

    void deleteByTenantIdAndId(TenantId tenantId, UserId id);
}
