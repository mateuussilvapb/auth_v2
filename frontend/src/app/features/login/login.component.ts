import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthApiService } from '../../core/services/auth-api.service';
import { AuthorizeContinuationService } from '../../core/services/authorize-continuation.service';
import { BrandingResponse, ApiErrorResponse } from '../../core/models/auth-api.models';

/**
 * Rota pública {@code /login} (seção 2.2/7.1 do plano) — não autentica via OAuth2. Envia
 * usuário/senha para {@code POST /api/auth/login} (sessão) e, em caso de sucesso, retoma
 * {@code GET /oauth2/authorize} preservado na query string desta rota.
 */
@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  clientId = '';
  usernameOrEmail = '';
  password = '';

  branding = signal<BrandingResponse | null>(null);
  errorMessage = signal<string | null>(null);
  submitting = signal(false);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authApi: AuthApiService,
    private readonly continuation: AuthorizeContinuationService,
  ) {}

  ngOnInit(): void {
    const clientId = this.route.snapshot.queryParamMap.get('client_id');
    if (!clientId) {
      // Sem client_id não há como resolver tenant nem retomar /oauth2/authorize — não é
      // um estado que a UI deveria tentar renderizar como formulário de login normal.
      this.errorMessage.set('Link de acesso inválido. Volte ao sistema de origem e tente novamente.');
      return;
    }

    this.clientId = clientId;
    this.authApi.branding(clientId).subscribe({
      next: (branding) => this.branding.set(branding),
      // client_id desconhecido: segue sem branding (nome/logo genéricos no template),
      // o próprio POST /api/auth/login vai rejeitar a tentativa de login mais adiante.
      error: () => this.branding.set(null),
    });
  }

  async submit(): Promise<void> {
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authApi.login({ clientId: this.clientId, usernameOrEmail: this.usernameOrEmail, password: this.password }).subscribe({
      next: async () => {
        const params = await this.continuation.captureParams();
        this.continuation.continueAuthorize(params);
      },
      error: (error: { error?: ApiErrorResponse }) => {
        this.submitting.set(false);
        this.errorMessage.set(error.error?.message ?? 'Não foi possível entrar. Tente novamente.');
      },
    });
  }
}
