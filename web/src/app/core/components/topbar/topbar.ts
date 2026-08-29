//Angular
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';

//Aplicação
import { ConsoleAuthService } from '../../services/console-auth.service';
import { TenantContextService } from '../../services/tenant-context.service';

//Externos
import { ButtonModule } from 'primeng/button';
import { ConfirmationService } from 'primeng/api';

/**
 * Topbar do console (guia de estilo, seção 7.1) — marca, tenant selecionado (clicável, leva
 * à troca — decisão de produto 2026-08-29) e sair. Alternador de tema entra junto do dark
 * mode (Fase 7, item pendente).
 */
@Component({
  selector: 'app-topbar',
  imports: [RouterLink, ButtonModule],
  templateUrl: './topbar.html',
})
export class Topbar {
  private readonly consoleAuth = inject(ConsoleAuthService);
  private readonly tenantContext = inject(TenantContextService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly router = inject(Router);

  readonly selectedTenant = this.tenantContext.selectedTenant;
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

  private doChangeTenant(): void {
    // Toda troca de tenant limpa o contexto persistido (decisão de produto 2026-08-29) —
    // a tela de seleção decide o próximo, não um valor anterior remanescente.
    this.tenantContext.clear();
    this.router.navigate(['/console/selecionar-tenant']);
  }

  logout(): void {
    this.tenantContext.clear();
    this.consoleAuth.logout();
  }
}
