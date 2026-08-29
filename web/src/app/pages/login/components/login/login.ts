//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

//Aplicação
import { AuthApiService } from '../../../../core/services/auth-api.service';
import { AuthorizeContinuationService } from '../../../../core/services/authorize-continuation.service';
import { BrandingResponse, ApiErrorResponse } from '../../../../core/models/auth-api.models';
import { FormLabel } from '../../../../shared/components/form-label/form-label';

//Externos
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

/**
 * Rota pública {@code /login} (seção 2.2/7.1 do plano) — não autentica via OAuth2. Envia
 * usuário/senha para {@code POST /api/auth/login} (sessão) e, em caso de sucesso, retoma
 * {@code GET /oauth2/authorize} preservado na query string desta rota.
 *
 * Guia de estilo, seção 6.1: a mensagem de erro é exibida **literalmente** como recebida
 * do backend — proibido diferenciar usuário inexistente de senha incorreta, por texto,
 * foco automático ou qualquer outro sinal.
 */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, PasswordModule, FormLabel],
  templateUrl: './login.html',
})
export class Login implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authApi = inject(AuthApiService);
  private readonly continuation = inject(AuthorizeContinuationService);
  private readonly fb = inject(FormBuilder);

  clientId = '';

  form = this.fb.group({
    usernameOrEmail: this.fb.control('', { validators: [Validators.required] }),
    password: this.fb.control('', { validators: [Validators.required] }),
  });

  branding = signal<BrandingResponse | null>(null);
  errorMessage = signal<string | null>(null);
  submitting = signal(false);

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
    if (this.form.invalid) {
      return;
    }

    this.errorMessage.set(null);
    this.submitting.set(true);

    const { usernameOrEmail, password } = this.form.getRawValue();

    this.authApi.login({ clientId: this.clientId, usernameOrEmail: usernameOrEmail!, password: password! }).subscribe({
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
