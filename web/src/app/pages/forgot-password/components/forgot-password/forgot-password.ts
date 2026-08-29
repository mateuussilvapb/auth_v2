//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

//Aplicação
import { AuthApiService } from '../../../../core/services/auth-api.service';
import { FormLabel } from '../../../../shared/components/form-label/form-label';

//Externos
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

/**
 * Rota pública {@code /esqueci-senha} (seção 2.2/7.1 do plano; guia de estilo 6.2).
 * {@code POST /api/auth/forgot-password} sempre responde 200 independente do usuário
 * existir, então a UI mostra a **mesma** mensagem de sucesso nos dois casos — nunca
 * diferenciar por texto, ícone ou tempo de resposta.
 */
@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, FormLabel],
  templateUrl: './forgot-password.html',
})
export class ForgotPassword implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authApi = inject(AuthApiService);
  private readonly fb = inject(FormBuilder);

  clientId = '';

  form = this.fb.group({
    usernameOrEmail: this.fb.control('', { validators: [Validators.required] }),
  });

  submitted = signal(false);
  submitting = signal(false);

  ngOnInit(): void {
    this.clientId = this.route.snapshot.queryParamMap.get('client_id') ?? '';
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }

    this.submitting.set(true);

    const { usernameOrEmail } = this.form.getRawValue();

    this.authApi.forgotPassword({ clientId: this.clientId, usernameOrEmail: usernameOrEmail! }).subscribe({
      // Mesma resposta em sucesso ou erro esperado (usuário inexistente): a API não
      // diferencia os dois casos, então a UI também não deve.
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
      },
      error: () => {
        this.submitting.set(false);
        this.submitted.set(true);
      },
    });
  }
}
