
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthApiService } from '../../core/services/auth-api.service';
import { ApiErrorResponse } from '../../core/models/auth-api.models';

/**
 * Rota pública {@code /reset-password?token=...} — destino do link enviado por
 * {@code POST /api/auth/forgot-password} (seção 7.4). {@code token} vem cru na URL (o
 * e-mail contém o valor bruto, não o hash armazenado em `password_reset_token`).
 */
@Component({
  selector: 'app-reset-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css',
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  newPassword = '';

  invalidLink = signal(false);
  success = signal(false);
  errorMessage = signal<string | null>(null);
  submitting = signal(false);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly authApi: AuthApiService,
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.invalidLink.set(true);
      return;
    }
    this.token = token;
  }

  submit(): void {
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authApi.resetPassword({ token: this.token, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.success.set(true);
      },
      error: (error: { error?: ApiErrorResponse }) => {
        this.submitting.set(false);
        this.errorMessage.set(error.error?.message ?? 'Não foi possível redefinir a senha. Tente novamente.');
      },
    });
  }
}
