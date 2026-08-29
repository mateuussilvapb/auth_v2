import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { ConsoleAuthService } from '../services/console-auth.service';

/**
 * Exige token válido do console administrativo (seção 2.2/D6, Fase 9) em toda rota sob
 * {@code /console} (exceto {@code /console/callback}, que não usa este guard). Sem token,
 * dispara o fluxo PKCE (`initCodeFlow`) — o browser navega para {@code /oauth2/authorize}
 * de página inteira, então o {@code false} retornado aqui nunca chega a importar para o
 * roteador Angular.
 */
export const consoleAuthGuard: CanActivateFn = () => {
  const consoleAuth = inject(ConsoleAuthService);
  return checkAuthenticated(consoleAuth);
};

async function checkAuthenticated(consoleAuth: ConsoleAuthService): Promise<boolean> {
  if (consoleAuth.isAuthenticated()) {
    return true;
  }

  await consoleAuth.login();
  return false;
}
