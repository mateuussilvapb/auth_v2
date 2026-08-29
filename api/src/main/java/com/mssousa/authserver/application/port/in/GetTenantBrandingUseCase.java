package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.model.TenantBranding;

/**
 * Porta de entrada para o endpoint público de branding (seção 7 do plano) — resolve o
 * tenant a partir do {@code client_id}, nunca de input do usuário, mesma regra da
 * autenticação (seção 2.2).
 */
public interface GetTenantBrandingUseCase {

    /**
     * @throws ResourceNotFoundException se o {@code client_id} não corresponder a nenhum
     *                                    sistema vinculado a um tenant. Diferente do login,
     *                                    não precisa de mensagem genérica — não há
     *                                    identidade de usuário para vazar aqui.
     */
    TenantBranding resolveByClientId(String clientId);
}
