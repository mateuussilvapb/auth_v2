import { Routes } from '@angular/router';

import { consoleAuthGuard } from './core/guards/console-auth.guard';

/**
 * Duas árvores (guia de estilo, seção 1.1/7.1; plano, Fase 5): pública — shell
 * `PublicLayout`, card centralizado, zero fricção — e `/console` — protegida por
 * {@code consoleAuthGuard}, com o shell `ConsoleLayout` (topbar + sidebar). Tudo lazy via
 * `loadComponent`; `/console/callback` fica fora do guard porque é o próprio destino do
 * fluxo PKCE (ainda sem token válido).
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./core/layout/public-layout/public-layout').then((m) => m.PublicLayout),
    children: [
      { path: 'login', loadComponent: () => import('./pages/login/components/login/login').then((m) => m.Login) },
      {
        path: 'consent',
        loadComponent: () => import('./pages/consent/components/consent/consent').then((m) => m.Consent),
      },
      {
        path: 'esqueci-senha',
        loadComponent: () =>
          import('./pages/forgot-password/components/forgot-password/forgot-password').then(
            (m) => m.ForgotPassword,
          ),
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./pages/reset-password/components/reset-password/reset-password').then((m) => m.ResetPassword),
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ],
  },
  {
    path: 'console/callback',
    loadComponent: () =>
      import('./pages/console/callback/components/console-callback/console-callback').then(
        (m) => m.ConsoleCallback,
      ),
  },
  {
    path: 'console',
    canActivate: [consoleAuthGuard],
    loadComponent: () => import('./core/layout/console-layout/console-layout').then((m) => m.ConsoleLayout),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/console/dashboard/components/console-dashboard/console-dashboard').then(
            (m) => m.ConsoleDashboard,
          ),
      },
      {
        path: 'tenants',
        loadComponent: () =>
          import('./pages/console/tenants/components/tenant-list/tenant-list').then((m) => m.TenantList),
      },
      {
        path: 'tenants/novo',
        loadComponent: () =>
          import('./pages/console/tenants/components/tenant-form/tenant-form').then((m) => m.TenantForm),
      },
      {
        path: 'tenants/:id/editar',
        loadComponent: () =>
          import('./pages/console/tenants/components/tenant-form/tenant-form').then((m) => m.TenantForm),
      },
      {
        path: 'tenants/:tenantId/systems',
        loadComponent: () =>
          import('./pages/console/systems/components/system-list/system-list').then((m) => m.SystemList),
      },
      {
        path: 'tenants/:tenantId/systems/novo',
        loadComponent: () =>
          import('./pages/console/systems/components/system-form/system-form').then((m) => m.SystemForm),
      },
      {
        path: 'systems/:systemId/profiles',
        loadComponent: () =>
          import('./pages/console/profiles/components/profile-list/profile-list').then((m) => m.ProfileList),
      },
      {
        path: 'tenants/:tenantId/users',
        loadComponent: () =>
          import('./pages/console/users/components/user-list/user-list').then((m) => m.UserList),
      },
      {
        path: 'tenants/:tenantId/users/novo',
        loadComponent: () =>
          import('./pages/console/users/components/user-form/user-form').then((m) => m.UserForm),
      },
      {
        path: 'tenants/:tenantId/users/:id/editar',
        loadComponent: () =>
          import('./pages/console/users/components/user-form/user-form').then((m) => m.UserForm),
      },
      {
        path: 'tenants/:tenantId/users/:userId/bindings',
        loadComponent: () =>
          import('./pages/console/bindings/components/user-bindings/user-bindings').then((m) => m.UserBindings),
      },
    ],
  },
];
