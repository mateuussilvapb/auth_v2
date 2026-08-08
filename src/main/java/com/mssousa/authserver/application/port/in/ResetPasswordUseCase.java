package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.domain.model.tenant.TenantId;

/**
 * Porta de entrada para o fluxo de "esqueci minha senha" (seção 7.4:
 * {@code POST /api/auth/forgot-password}, {@code POST /api/auth/reset-password}).
 */
public interface ResetPasswordUseCase {

    /**
     * Solicita a redefinição. Sempre "sucede" silenciosamente, exista ou não o usuário —
     * o e-mail só é enviado se existir, mas o chamador nunca recebe essa informação
     * (seção 6.6: falha de autenticação/identidade nunca revela existência de usuário).
     */
    void requestReset(TenantId tenantId, String usernameOrEmail);

    /**
     * Confirma a redefinição a partir do token em texto plano recebido por e-mail.
     *
     * @throws com.mssousa.authserver.domain.exception.DomainException se o token for
     *                                                                  inválido, expirado ou já utilizado — mensagem sempre genérica, sem
     *                                                                  distinguir os três casos
     */
    void confirmReset(String rawToken, String newPassword);
}
