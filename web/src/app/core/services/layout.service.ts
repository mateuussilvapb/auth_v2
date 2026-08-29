//Angular
import { toSignal } from '@angular/core/rxjs-interop';
import { inject, Injectable, signal, computed } from '@angular/core';

//Aplicação
import { ScreenSizeService } from './screen-size.service';

interface LayoutState {
  mainMenuVisible: boolean;
}

/**
 * Estado do shell do console (guia de estilo, seção 7.1) — responsividade do menu lateral,
 * portado da referência.
 */
@Injectable({
  providedIn: 'root',
})
export class LayoutService {
  private readonly screenSizeService = inject(ScreenSizeService);

  private readonly width = toSignal(this.screenSizeService.width$, {
    initialValue: window.innerWidth,
  });

  state = signal<LayoutState>({
    mainMenuVisible: this.width() > 991,
  });

  onMenuToggle(): void {
    this.state.update((state) => ({ ...state, mainMenuVisible: !state.mainMenuVisible }));
  }

  isDesktop = computed(() => this.width() > 991);
  mainMenuVisible = computed(() => this.state().mainMenuVisible);
}
