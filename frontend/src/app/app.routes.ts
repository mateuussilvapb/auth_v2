import { Routes } from '@angular/router';

import { LoginComponent } from './features/login/login.component';
import { ConsentComponent } from './features/consent/consent.component';
import { ForgotPasswordComponent } from './features/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/reset-password/reset-password.component';
import { ConsoleCallbackComponent } from './features/console/console-callback.component';
import { ConsoleDashboardComponent } from './features/console/console-dashboard.component';
import { consoleAuthGuard } from './core/guards/console-auth.guard';

/**
 * Rotas públicas (seção 2.2/9 do plano): /login, /consent, /esqueci-senha,
 * /reset-password. Rotas protegidas do console administrativo sob /console (seção 2.2/D6)
 * — client OAuth2 PKCE estático (RegisteredClientRepositoryConfig no backend), platform
 * admin autenticado via consoleAuthGuard. As telas de CRUD (tenants/sistemas/perfis/
 * usuários/vínculos) ainda não existem — próximos itens do checklist da Fase 9.
 */
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'consent', component: ConsentComponent },
  { path: 'esqueci-senha', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'console/callback', component: ConsoleCallbackComponent },
  { path: 'console', component: ConsoleDashboardComponent, canActivate: [consoleAuthGuard] },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
