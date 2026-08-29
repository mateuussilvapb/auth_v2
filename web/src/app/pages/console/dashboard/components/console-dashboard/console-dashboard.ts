import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { LayoutBasePages } from '../../../../../shared/components/layout-base-pages/layout-base-pages';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { ConsoleAuthService } from '../../../../../core/services/console-auth.service';
import { TenantContextService } from '../../../../../core/services/tenant-context.service';
import { decodeJwtPayload } from '../../../../../core/util/jwt';

const SESSION_CHECK_INTERVAL_MS = 30_000;

/**
 * Landing page do console administrativo, protegida por {@code consoleAuthGuard} e
 * {@code tenantContextGuard} (sempre há um tenant selecionado ao chegar aqui). Mostra
 * contagens do tenant corrente e atalhos para as telas já existentes (Tenants, Sistemas,
 * Usuários).
 * <p>
 * Corrige o bug de UX registrado em `api/PROGRESS.md` (2026-08-29): o dashboard continuava
 * exibindo "Logado como X" mesmo com o access token expirado, porque só decodificava o JWT
 * sem checar validade. Aqui o estado de autenticação deriva de
 * {@link ConsoleAuthService#isAuthenticated}, que compara `expires_at` (guia de estilo, seção
 * 6.5) — checado ao entrar na tela e reavaliado periodicamente enquanto o platform admin fica
 * parado nela (não há navegação para o guard reavaliar sozinho).
 * </p>
 */
@Component({
  selector: 'app-console-dashboard',
  imports: [ButtonModule, LayoutBasePages],
  templateUrl: './console-dashboard.html',
})
export class ConsoleDashboard implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly consoleAuth = inject(ConsoleAuthService);
  private readonly tenantContext = inject(TenantContextService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  readonly selectedTenant = this.tenantContext.selectedTenant;

  sessionExpired = signal(false);
  name = signal<string | null>(null);
  username = signal<string | null>(null);

  tenantsTotal = signal<number | null>(null);
  tenantsError = signal(false);
  systemsTotal = signal<number | null>(null);
  systemsError = signal(false);
  usersTotal = signal<number | null>(null);
  usersError = signal(false);

  ngOnInit(): void {
    if (!this.checkSession()) {
      return;
    }

    const claims = decodeJwtPayload(this.consoleAuth.getAccessToken());
    this.name.set(typeof claims['name'] === 'string' ? (claims['name'] as string) : null);
    this.username.set(typeof claims['username'] === 'string' ? (claims['username'] as string) : null);

    this.loadCounts();

    const intervalId = setInterval(() => this.checkSession(), SESSION_CHECK_INTERVAL_MS);
    this.destroyRef.onDestroy(() => clearInterval(intervalId));
  }

  reauthenticate(): void {
    this.consoleAuth.login();
  }

  goToTenants(): void {
    this.router.navigate(['/console/tenants']);
  }

  goToSystems(tenantId: string): void {
    this.router.navigate(['/console/tenants', tenantId, 'systems']);
  }

  goToUsers(tenantId: string): void {
    this.router.navigate(['/console/tenants', tenantId, 'users']);
  }

  private checkSession(): boolean {
    const authenticated = this.consoleAuth.isAuthenticated();
    this.sessionExpired.set(!authenticated);
    return authenticated;
  }

  private loadCounts(): void {
    const tenantId = this.selectedTenant()?.id;

    this.adminApi.listTenants(0, 1).subscribe({
      next: (result) => this.tenantsTotal.set(result.totalElements),
      error: () => this.tenantsError.set(true),
    });

    if (!tenantId) {
      return;
    }

    this.adminApi.listSystemsByTenant(tenantId, 0, 1).subscribe({
      next: (result) => this.systemsTotal.set(result.totalElements),
      error: () => this.systemsError.set(true),
    });

    this.adminApi.listUsers(tenantId, 0, 1).subscribe({
      next: (result) => this.usersTotal.set(result.totalElements),
      error: () => this.usersError.set(true),
    });
  }
}
