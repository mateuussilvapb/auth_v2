
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AdminApiService } from '../../../core/services/admin-api.service';
import { UserResponse } from '../../../core/models/admin-api.models';

/**
 * Rota {@code /console/tenants/:tenantId/users} — usuário é escopado a exatamente um
 * tenant (seção 3.2 do plano), então a listagem sempre parte de um tenant.
 * {@code UserStatus} tem 3 valores (ACTIVE/BLOCKED/DISABLED, seção 3.4), não é um toggle
 * binário como Tenant/System/SystemProfile — usa {@code <select>}.
 */
@Component({
  selector: 'app-user-list',
  imports: [RouterLink],
  templateUrl: './user-list.component.html',
})
export class UserListComponent implements OnInit {
  tenantId = '';
  users = signal<UserResponse[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  infoMessage = signal<string | null>(null);
  page = signal(0);
  totalPages = signal(0);

  readonly statuses = ['ACTIVE', 'BLOCKED', 'DISABLED'];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly adminApi: AdminApiService,
  ) {}

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.paramMap.get('tenantId') ?? '';
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminApi.listUsers(this.tenantId, this.page(), 20).subscribe({
      next: (result) => {
        this.users.set(result.content);
        this.totalPages.set(result.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os usuários.');
        this.loading.set(false);
      },
    });
  }

  changeStatus(user: UserResponse, status: string): void {
    this.adminApi.updateUserStatus(this.tenantId, user.id, { status }).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Não foi possível alterar o status do usuário.'),
    });
  }

  resetPassword(user: UserResponse): void {
    this.infoMessage.set(null);
    this.adminApi.resetUserPassword(this.tenantId, user.id).subscribe({
      next: () => this.infoMessage.set(`E-mail de redefinição enviado para ${user.email}.`),
      error: () => this.errorMessage.set('Não foi possível iniciar a redefinição de senha.'),
    });
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.load();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.set(this.page() + 1);
      this.load();
    }
  }
}
