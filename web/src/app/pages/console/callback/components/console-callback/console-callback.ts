
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ConsoleAuthService } from '../../../../../core/services/console-auth.service';
import { TenantContextService } from '../../../../../core/services/tenant-context.service';

/**
 * Rota {@code /console/callback} — destino do redirect_uri do client PKCE estático do
 * console (seção 2.2/D6, Fase 9). {@code tryLoginCodeFlow} troca o {@code code} por token
 * (com o {@code code_verifier} guardado localmente pela lib desde {@code initCodeFlow}) e
 * navega para o dashboard do console.
 */
@Component({
  selector: 'app-console-callback',
  imports: [],
  template: `
    @if (errorMessage()) {
      <p role="alert">{{ errorMessage() }}</p>
    } @else {
      <p>Entrando...</p>
    }
  `,
})
export class ConsoleCallback implements OnInit {
  errorMessage = signal<string | null>(null);

  constructor(
    private readonly consoleAuth: ConsoleAuthService,
    private readonly tenantContext: TenantContextService,
    private readonly router: Router,
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      await this.consoleAuth.completeLoginFlow();
      if (this.consoleAuth.isAuthenticated()) {
        // Todo login exige nova seleção de tenant (decisão de produto 2026-08-29), mesmo que
        // já exista um contexto persistido de uma sessão anterior.
        this.tenantContext.clear();
        await this.router.navigate(['/console']);
      } else {
        this.errorMessage.set('Não foi possível concluir o login do console.');
      }
    } catch {
      this.errorMessage.set('Não foi possível concluir o login do console.');
    }
  }
}
