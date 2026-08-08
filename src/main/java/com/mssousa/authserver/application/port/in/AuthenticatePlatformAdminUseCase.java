package com.mssousa.authserver.application.port.in;

import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;

/**
 * Porta de entrada para autenticação do platform admin — o "usuário deus" que opera
 * acima de todos os tenants (seção 2.1). Não há tenant nem sistema envolvidos.
 */
public interface AuthenticatePlatformAdminUseCase {

    /**
     * @throws AuthenticationFailedException com mensagem sempre genérica (seção 6.6)
     */
    PlatformAdmin authenticate(String usernameOrEmail, String plainPassword);
}
