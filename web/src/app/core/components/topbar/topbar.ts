//Angular
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';

//Aplicação
import { ConsoleAuthService } from '../../services/console-auth.service';
import { TenantContextService } from '../../services/tenant-context.service';
import { ThemeService } from '../../services/theme.service';

//Externos
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService } from 'primeng/api';

/**
 * Topbar do console (guia de estilo, seção 7.1) — marca, tenant selecionado (clicável, leva
 * à troca — decisão de produto 2026-08-29), alternador de tema ({@link ThemeService}) e
 * sair.
 */
@Component({
  selector: 'app-topbar',
  imports: [RouterLink, ButtonModule, TooltipModule],
  templateUrl: './topbar.html',
})
export class Topbar {
  private readonly consoleAuth = inject(ConsoleAuthService);
  private readonly tenantContext = inject(TenantContextService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly router = inject(Router);
  private readonly themeService = inject(ThemeService);

  readonly selectedTenant = this.tenantContext.selectedTenant;
  readonly dark = this.themeService.dark;
  private readonly isCriticalScreen = signal(false);

  constructor() {
    this.updateCriticalFlag();
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.updateCriticalFlag());
  }

  private updateCriticalFlag(): void {
    let current: ActivatedRouteSnapshot | null = this.router.routerState.snapshot.root;
    let critical = false;
    while (current) {
      if (current.data['critical']) {
        critical = true;
        break;
      }
      current = current.firstChild;
    }
    this.isCriticalScreen.set(critical);
  }

  changeTenant(): void {
    if (this.isCriticalScreen()) {
      this.confirmationService.confirm({
        header: 'Trocar de tenant',
        message:
          'Você está em uma tela de cadastro ou edição. Trocar de tenant agora descarta as informações não salvas. Deseja continuar?',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Trocar tenant',
        rejectLabel: 'Cancelar',
        accept: () => this.doChangeTenant(),
      });
      return;
    }

    this.doChangeTenant();
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }

  private doChangeTenant(): void {
    // Toda troca de tenant limpa o contexto persistido (decisão de produto 2026-08-29) —
    // a tela de seleção decide o próximo, não um valor anterior remanescente.
    this.tenantContext.clear();
    this.router.navigate(['/console/selecionar-tenant']);
  }

  async logout(): Promise<void> {
    this.tenantContext.clear();
    // Aguarda a invalidação da sessão no backend antes de recarregar — sem esperar, o
    // próximo GET /oauth2/authorize poderia correr antes do POST /api/auth/logout terminar
    // e reautenticar silenciosamente com a sessão ainda viva.
    await this.consoleAuth.logout();
    // Navegação de página inteira (não `router.navigate`) — o app já está em `/console`, e o
    // router não reavalia guards numa navegação para a mesma URL. Recarregar do zero também
    // garante que nenhum estado de componente em memória sobreviva ao logout.
    window.location.href = '/console';
  }
}
