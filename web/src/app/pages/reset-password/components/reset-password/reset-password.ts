//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

//Aplicação
import { AuthApiService } from '../../../../core/services/auth-api.service';
import { ApiErrorResponse } from '../../../../core/models/auth-api.models';
import { FormLabel } from '../../../../shared/components/form-label/form-label';

//Externos
import { ButtonModule } from 'primeng/button';
import { PasswordModule } from 'primeng/password';

/**
 * Rota pública {@code /reset-password?token=...} — destino do link enviado por
 * {@code POST /api/auth/forgot-password} (seção 7.4 do plano). {@code token} vem cru na
 * URL (o e-mail contém o valor bruto, não o hash armazenado em `password_reset_token`).
 */
@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, PasswordModule, FormLabel],
  templateUrl: './reset-password.html',
})
export class ResetPassword implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authApi = inject(AuthApiService);
  private readonly fb = inject(FormBuilder);

  token = '';

  form = this.fb.group({
    newPassword: this.fb.control('', { validators: [Validators.required, Validators.minLength(8)] }),
  });

  errorMessages = {
    required: 'Este campo é obrigatório.',
    minlength: 'A senha deve ter pelo menos 8 caracteres.',
  };

  invalidLink = signal(false);
  success = signal(false);
  errorMessage = signal<string | null>(null);
  submitting = signal(false);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.invalidLink.set(true);
      return;
    }
    this.token = token;
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }

    this.errorMessage.set(null);
    this.submitting.set(true);

    const { newPassword } = this.form.getRawValue();

    this.authApi.resetPassword({ token: this.token, newPassword: newPassword! }).subscribe({
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
