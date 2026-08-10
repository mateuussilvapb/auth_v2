
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ConsoleAuthService } from '../../core/services/console-auth.service';

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
export class ConsoleCallbackComponent implements OnInit {
  errorMessage = signal<string | null>(null);

  constructor(
    private readonly consoleAuth: ConsoleAuthService,
    private readonly router: Router,
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      await this.consoleAuth.completeLoginFlow();
      if (this.consoleAuth.isAuthenticated()) {
        await this.router.navigate(['/console']);
      } else {
        this.errorMessage.set('Não foi possível concluir o login do console.');
      }
    } catch {
      this.errorMessage.set('Não foi possível concluir o login do console.');
    }
  }
}
