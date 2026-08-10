
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { ApiErrorResponse } from '../../../../../core/models/auth-api.models';

/**
 * Rotas {@code /console/tenants/:tenantId/users/novo} e
 * {@code /console/tenants/:tenantId/users/:id/editar}. {@code username} é imutável após a
 * criação (só {@code name}/{@code email} entram em {@code UpdateUserRequest}) — a senha
 * nunca é editada por aqui, só via "Redefinir senha" (fluxo de e-mail, seção 7.4).
 */
@Component({
  selector: 'app-user-form',
  imports: [FormsModule, RouterLink],
  templateUrl: './user-form.html',
})
export class UserForm implements OnInit {
  tenantId = '';
  userId: string | null = null;
  username = '';
  email = '';
  password = '';
  name = '';

  loading = signal(false);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly adminApi: AdminApiService,
  ) {}

  get isEditing(): boolean {
    return this.userId !== null;
  }

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.paramMap.get('tenantId') ?? '';
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    this.userId = idParam;
    this.loading.set(true);
    this.adminApi.getUser(this.tenantId, this.userId).subscribe({
      next: (user) => {
        this.username = user.username;
        this.email = user.email;
        this.name = user.name;
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar o usuário.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    this.errorMessage.set(null);
    this.submitting.set(true);

    const request$ = this.isEditing
      ? this.adminApi.updateUser(this.tenantId, this.userId!, { name: this.name, email: this.email })
      : this.adminApi.createUser(this.tenantId, {
          username: this.username,
          email: this.email,
          password: this.password,
          name: this.name,
        });

    request$.subscribe({
      next: () => this.router.navigate(['/console/tenants', this.tenantId, 'users']),
      error: (error: { error?: ApiErrorResponse }) => {
        this.submitting.set(false);
        this.errorMessage.set(error.error?.message ?? 'Não foi possível salvar o usuário.');
      },
    });
  }
}
