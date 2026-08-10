import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // withFetch: cookie de sessão HttpOnly (seção 7.1) precisa ir em toda chamada às
    // rotas /api/auth/** — os serviços fazem isso explicitamente via { withCredentials: true }.
    provideHttpClient(withFetch()),
    // Cliente OAuth2 PKCE do console administrativo (seção 2.2/D6, Fase 9) — configurado
    // em ConsoleAuthService, não aqui (depende de environment.apiBaseUrl resolvido em
    // runtime, e só é usado sob /console).
    provideOAuthClient(),
  ],
};
