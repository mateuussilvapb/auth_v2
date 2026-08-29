//Angular
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

//Aplicação
import { Topbar } from '../../components/topbar/topbar';
import { Sidebar } from '../../components/sidebar/sidebar';
import { Toast } from '../../../shared/components/toast/toast';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingOverlay } from '../../../shared/components/loading-overlay/loading-overlay';

/**
 * Shell do console administrativo (guia de estilo, seção 1.1/7.1) — topbar + sidebar +
 * router-outlet. Audiência: platform admin, uso contínuo, densidade de informação.
 * <p>
 * A sidebar some em rotas marcadas com {@code data: { hideSidebar: true }} — hoje só a
 * seleção de tenant, onde nenhum item do menu tem contexto pra navegar (decisão de produto
 * 2026-08-29).
 */
@Component({
  selector: 'app-console-layout',
  imports: [RouterOutlet, Topbar, Sidebar, Toast, ConfirmDialog, LoadingOverlay],
  templateUrl: './console-layout.html',
})
export class ConsoleLayout {
  private readonly router = inject(Router);

  protected readonly showSidebar = signal(true);

  constructor() {
    this.updateShowSidebar();
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.updateShowSidebar());
  }

  private updateShowSidebar(): void {
    let current: ActivatedRouteSnapshot | null = this.router.routerState.snapshot.root;
    let hideSidebar = false;
    while (current) {
      if (current.data['hideSidebar']) {
        hideSidebar = true;
        break;
      }
      current = current.firstChild;
    }
    this.showSidebar.set(!hideSidebar);
  }
}
