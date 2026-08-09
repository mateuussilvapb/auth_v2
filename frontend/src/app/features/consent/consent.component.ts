import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AuthApiService } from '../../core/services/auth-api.service';
import { AuthorizeContinuationService } from '../../core/services/authorize-continuation.service';
import { ApiErrorResponse } from '../../core/models/auth-api.models';

/**
 * Rota pública {@code /consent} (seção 2.2 do plano) — destino do
 * {@code authorizationEndpoint.consentPage("/consent")} do backend. O Authorization Server
 * redireciona para cá com {@code client_id}/{@code state}/{@code scope} (space-separated) na
 * query string quando o `System` é de terceiro (`thirdParty = true`) e ainda não há
 * consentimento salvo para esse par (client, usuário). `POST /api/auth/consent` só grava o
 * consentimento; é este componente que retoma {@code GET /oauth2/authorize} na sequência.
 */
@Component({
  selector: 'app-consent',
  imports: [CommonModule],
  templateUrl: './consent.component.html',
  styleUrl: './consent.component.css',
})
export class ConsentComponent implements OnInit {
  clientId = '';
  scopes: string[] = [];

  errorMessage = signal<string | null>(null);
  submitting = signal(false);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly authApi: AuthApiService,
    private readonly continuation: AuthorizeContinuationService,
  ) {}

  ngOnInit(): void {
    const clientId = this.route.snapshot.queryParamMap.get('client_id');
    const scope = this.route.snapshot.queryParamMap.get('scope');
    if (!clientId || !scope) {
      this.errorMessage.set('Link de consentimento inválido. Volte ao sistema de origem e tente novamente.');
      return;
    }

    this.clientId = clientId;
    this.scopes = scope.split(' ').filter((s) => s.length > 0);
  }

  async authorize(): Promise<void> {
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authApi.consent({ clientId: this.clientId, scopes: this.scopes }).subscribe({
      next: async () => {
        const params = await this.continuation.captureParams();
        this.continuation.continueAuthorize(params);
      },
      error: (error: { error?: ApiErrorResponse }) => {
        this.submitting.set(false);
        this.errorMessage.set(error.error?.message ?? 'Não foi possível registrar o consentimento. Tente novamente.');
      },
    });
  }
}
