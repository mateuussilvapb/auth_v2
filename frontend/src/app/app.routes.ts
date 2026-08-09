import { Routes } from '@angular/router';

import { LoginComponent } from './features/login/login.component';
import { ConsentComponent } from './features/consent/consent.component';
import { ForgotPasswordComponent } from './features/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/reset-password/reset-password.component';

/**
 * Rotas públicas (seção 2.2/9 do plano): /login, /consent, /esqueci-senha,
 * /reset-password. As rotas protegidas do console administrativo (Fase 8, PKCE via
 * angular-oauth2-oidc) entram sob /console num commit posterior desta mesma fase.
 */
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'consent', component: ConsentComponent },
  { path: 'esqueci-senha', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
