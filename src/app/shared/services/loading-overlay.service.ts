//Angular
import { Injectable, computed, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LoadingOverlayService {
  private readonly pendentes = signal<number>(0);

  readonly visible = computed(() => this.pendentes() > 0);

  show(): void {
    this.pendentes.update((valor) => valor + 1);
  }

  hide(): void {
    this.pendentes.update((valor) => Math.max(0, valor - 1));
  }

  async wrap<T>(task: () => Promise<T>): Promise<T> {
    this.show();
    try {
      return await task();
    } finally {
      this.hide();
    }
  }
}
