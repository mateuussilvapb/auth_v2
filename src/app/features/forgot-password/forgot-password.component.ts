
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthApiService } from '../../core/services/auth-api.service';

/**
 * Rota pública {@code /esqueci-senha} (seção 2.2/7.1 do plano). {@code POST
 * /api/auth/forgot-password} sempre responde 200 independente do usuário existir (seção
 * 7.4 — evitar enumeração de contas), então a UI mostra a mesma mensagem de sucesso em
 * ambos os casos.
 */
@Component({
  selector: 'app-forgot-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css',
})
export class ForgotPasswordComponent implements OnInit {
  clientId = '';
  usernameOrEmail = '';

  submitted = signal(false);
  submitting = signal(false);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly authApi: AuthApiService,
  ) {}

  ngOnInit(): void {
    this.clientId = this.route.snapshot.queryParamMap.get('client_id') ?? '';
  }

  submit(): void {
    this.submitting.set(true);

    this.authApi.forgotPassword({ clientId: this.clientId, usernameOrEmail: this.usernameOrEmail }).subscribe({
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
