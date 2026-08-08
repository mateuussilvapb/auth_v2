import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.domain.model.tenant.TenantId;
import com.mssousa.authserver.domain.model.user.UserId;

/**
 * Porta de entrada para o fluxo de "esqueci minha senha" (seção 7.4:
 * {@code POST /api/auth/forgot-password}, {@code POST /api/auth/reset-password}) e para o
 * reset administrativo (seção 9: {@code POST /admin/api/v1/users/{id}/reset-password}).
 */
public interface ResetPasswordUseCase {

    /**
     * Solicita a redefinição. O tenant é resolvido a partir do {@code client_id} — nunca
     * de input do usuário (mesma regra do login, seção 2.2). Sempre "sucede"
     * silenciosamente, exista ou não o client_id/tenant/usuário — o e-mail só é enviado se
     * tudo existir, mas o chamador nunca recebe essa informação (seção 6.6: falha de
     * autenticação/identidade nunca revela existência de usuário).
     */
    void requestReset(String clientId, String usernameOrEmail);

    /**
     * Variante administrativa: o platform admin já identificou o usuário por ID (não por
     * client_id/username), então não há necessidade — nem sentido — de sucesso
     * silencioso. Usada por {@code POST /admin/api/v1/users/{id}/reset-password}.
     *
     * @throws ResourceNotFoundException se o usuário não existir no tenant informado
     */
    void requestResetForUser(TenantId tenantId, UserId userId);

    /**
     * Confirma a redefinição a partir do token em texto plano recebido por e-mail.
     *
     * @throws com.mssousa.authserver.domain.exception.DomainException se o token for
     *                                                                  inválido, expirado ou já utilizado — mensagem sempre genérica, sem
     *                                                                  distinguir os três casos
     */
    void confirmReset(String rawToken, String newPassword);
}
