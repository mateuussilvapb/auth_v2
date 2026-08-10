import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { routes } from './app.routes';
import { PRIMENG_PROVIDER } from './core/config/providers/primeng.provider';

registerLocaleData(localePt, 'pt-BR');

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
    PRIMENG_PROVIDER,
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    MessageService,
    ConfirmationService,
    DialogService,
  ],
};
