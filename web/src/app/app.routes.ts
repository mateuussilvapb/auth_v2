import { Routes } from '@angular/router';

import { consoleAuthGuard } from './core/guards/console-auth.guard';
import { tenantContextGuard } from './core/guards/tenant-context.guard';

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
        canActivate: [tenantContextGuard],
        loadComponent: () =>
          import('./pages/console/dashboard/components/console-dashboard/console-dashboard').then(
            (m) => m.ConsoleDashboard,
          ),
      },
      {
        // Fora do tenantContextGuard: é o próprio destino do guard quando não há tenant
        // selecionado (decisão de produto 2026-08-29) — aplicá-lo aqui criaria loop.
        path: 'selecionar-tenant',
        // Sem tenant selecionado ainda não há contexto pra nenhum item do menu navegar
        // (decisão de produto 2026-08-29) — o ConsoleLayout lê esse data para ocultar a sidebar.
        data: { hideSidebar: true },
        loadComponent: () =>
          import('./pages/console/tenant-selection/components/tenant-selection/tenant-selection').then(
            (m) => m.TenantSelection,
          ),
      },
      {
        // Gestão de tenants fica fora do tenantContextGuard de propósito: é a única área
        // navegável antes de existir o primeiro tenant (guard redireciona pra cá nesse caso).
        path: 'tenants',
        loadComponent: () =>
          import('./pages/console/tenants/components/tenant-list/tenant-list').then((m) => m.TenantList),
      },
      {
        path: 'tenants/novo',
        data: { critical: true, formMode: 'cadastro' },
        loadComponent: () =>
          import('./pages/console/tenants/components/tenant-form/tenant-form').then((m) => m.TenantForm),
      },
      {
        path: 'tenants/:id/editar',
        data: { critical: true, formMode: 'edicao' },
        loadComponent: () =>
          import('./pages/console/tenants/components/tenant-form/tenant-form').then((m) => m.TenantForm),
      },
      {
        path: 'tenants/:tenantId/systems',
        canActivate: [tenantContextGuard],
        loadComponent: () =>
          import('./pages/console/systems/components/system-list/system-list').then((m) => m.SystemList),
      },
      {
        path: 'tenants/:tenantId/systems/novo',
        canActivate: [tenantContextGuard],
        data: { critical: true },
        loadComponent: () =>
          import('./pages/console/systems/components/system-form/system-form').then((m) => m.SystemForm),
      },
      {
        path: 'systems/:systemId/profiles',
        canActivate: [tenantContextGuard],
        loadComponent: () =>
          import('./pages/console/profiles/components/profile-list/profile-list').then((m) => m.ProfileList),
      },
      {
        path: 'tenants/:tenantId/users',
        canActivate: [tenantContextGuard],
        loadComponent: () =>
          import('./pages/console/users/components/user-list/user-list').then((m) => m.UserList),
      },
      {
        path: 'tenants/:tenantId/users/novo',
        canActivate: [tenantContextGuard],
        data: { critical: true },
        loadComponent: () =>
          import('./pages/console/users/components/user-form/user-form').then((m) => m.UserForm),
      },
      {
        path: 'tenants/:tenantId/users/:id/editar',
        canActivate: [tenantContextGuard],
        data: { critical: true },
        loadComponent: () =>
          import('./pages/console/users/components/user-form/user-form').then((m) => m.UserForm),
      },
      {
        path: 'tenants/:tenantId/users/:userId/bindings',
        canActivate: [tenantContextGuard],
        loadComponent: () =>
          import('./pages/console/bindings/components/user-bindings/user-bindings').then((m) => m.UserBindings),
      },
    ],
  },
];
