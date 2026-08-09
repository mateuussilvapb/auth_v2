import { Routes } from '@angular/router';

import { LoginComponent } from './features/login/login.component';

/**
 * Rotas públicas (seção 2.2/9 do plano): /login, /consent, /esqueci-senha. As rotas
 * protegidas do console administrativo (Fase 8, PKCE via angular-oauth2-oidc) entram sob
 * /console num commit posterior desta mesma fase.
 */
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
