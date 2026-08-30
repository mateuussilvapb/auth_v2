import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'console.theme';

/**
 * Preferência de tema do console (guia de estilo — decisão de produto 2026-08-30, plano
 * seção 5 "Alternador de tema: persistir onde?"). Persiste só em {@link localStorage} sob
 * uma chave namespaced ao console — nunca lida pelas telas públicas, que são visitadas por
 * usuários de tenants distintos no mesmo domínio e seguem só `prefers-color-scheme`, sem
 * persistência (evita que a preferência de um usuário vaze para os próximos que abrirem uma
 * tela pública no mesmo navegador).
 * <p>
 * O toggle de classe `.app-dark` (`darkModeSelector` do preset PrimeNG,
 * `core/config/providers/primeng.provider.ts`) fica restrito ao tempo de vida do
 * `ConsoleLayout` — aplicado só enquanto o platform admin está dentro do console, removido
 * ao sair, para as telas públicas nunca herdarem a escolha explícita do console.
 * </p>
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly dark = signal(this.resolveInitial());

  toggle(): void {
    this.set(!this.dark());
  }

  private set(dark: boolean): void {
    this.dark.set(dark);
    localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light');
  }

  private resolveInitial(): boolean {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'dark') {
      return true;
    }
    if (stored === 'light') {
      return false;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
}
