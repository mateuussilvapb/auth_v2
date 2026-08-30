//Angular
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

//Aplicação
import { Topbar } from '../../components/topbar/topbar';
import { Sidebar } from '../../components/sidebar/sidebar';
import { Toast } from '../../../shared/components/toast/toast';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingOverlay } from '../../../shared/components/loading-overlay/loading-overlay';
import { ThemeService } from '../../services/theme.service';

const DARK_CLASS = 'app-dark';
const LIGHT_CLASS = 'app-light';

/**
 * Shell do console administrativo (guia de estilo, seção 1.1/7.1) — topbar + sidebar +
 * router-outlet. Audiência: platform admin, uso contínuo, densidade de informação.
 * <p>
 * A sidebar some em rotas marcadas com {@code data: { hideSidebar: true }} — hoje só a
 * seleção de tenant, onde nenhum item do menu tem contexto pra navegar (decisão de produto
 * 2026-08-29).
 * </p>
 * <p>
 * `<html>` sempre ganha `.app-dark` OU `.app-light` ({@link ThemeService}) enquanto este
 * shell está montado — nunca nenhuma das duas, para "escolhi claro" se distinguir de "nunca
 * fui tocado" (ver `base.scss`) — e ambas são removidas no destroy para as telas públicas
 * nunca herdarem a preferência explícita do console (plano, seção 5).
 * </p>
 */
@Component({
  selector: 'app-console-layout',
  imports: [RouterOutlet, Topbar, Sidebar, Toast, ConfirmDialog, LoadingOverlay],
  templateUrl: './console-layout.html',
})
export class ConsoleLayout {
  private readonly router = inject(Router);
  private readonly themeService = inject(ThemeService);

  protected readonly showSidebar = signal(true);

  constructor() {
    this.updateShowSidebar();
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.updateShowSidebar());

    effect(() => {
      const dark = this.themeService.dark();
      document.documentElement.classList.toggle(DARK_CLASS, dark);
      document.documentElement.classList.toggle(LIGHT_CLASS, !dark);
    });
    inject(DestroyRef).onDestroy(() =>
      document.documentElement.classList.remove(DARK_CLASS, LIGHT_CLASS),
    );
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
