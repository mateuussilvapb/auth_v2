//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

//Aplicação
import { ListBase } from '../../../../../shared/components/list-base/list-base';
import { StatusTag } from '../../../../../shared/components/status-tag/status-tag';
import { LayoutBasePages } from '../../../../../shared/components/layout-base-pages/layout-base-pages';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { UserResponse } from '../../../../../core/models/admin-api.models';

//Externos
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule, TablePageEvent } from 'primeng/table';

const PAGE_SIZE = 10;

interface StatusAction {
  label: string;
  target: string;
  severity: 'secondary' | 'danger';
  confirm: boolean;
}

/**
 * Rota {@code /console/tenants/:tenantId/users} (guia de estilo, seção 5.3) — usuário é
 * escopado a exatamente um tenant (seção 3.2 do plano). {@code UserStatus} tem 3 valores
 * (ACTIVE/BLOCKED/DISABLED, seção 3.4), não é um toggle binário como Tenant/System/
 * SystemProfile — cada linha mostra só as transições relevantes para o status atual.
 */
@Component({
  selector: 'app-user-list',
  imports: [TableModule, ButtonModule, SkeletonModule, StatusTag, LayoutBasePages],
  templateUrl: './user-list.html',
})
export class UserList extends ListBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly activatedRoute = inject(ActivatedRoute);

  tenantId = '';
  users = signal<UserResponse[]>([]);
  totalRecords = signal(0);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  page = signal(0);
  readonly pageSize = PAGE_SIZE;
  readonly skeletonRows = Array.from({ length: 5 }, (_, i) => i);

  ngOnInit(): void {
    this.tenantId = this.activatedRoute.snapshot.paramMap.get('tenantId') ?? '';
    this.load(0);
  }

  load(page: number): void {
    this.page.set(page);
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminApi.listUsers(this.tenantId, page, this.pageSize).subscribe({
      next: (result) => {
        this.users.set(result.content);
        this.totalRecords.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os usuários.');
        this.loading.set(false);
      },
    });
  }

  onPage(event: TablePageEvent): void {
    this.load(Math.floor(event.first / event.rows));
  }

  goToCreate(): void {
    this.router.navigate(['/console/tenants', this.tenantId, 'users', 'novo']);
  }

  goToEdit(user: UserResponse): void {
    this.router.navigate(['/console/tenants', this.tenantId, 'users', user.id, 'editar']);
  }

  goToBindings(user: UserResponse): void {
    this.router.navigate(['/console/tenants', this.tenantId, 'users', user.id, 'bindings']);
  }

  statusActionsFor(user: UserResponse): StatusAction[] {
    switch (user.status) {
      case 'ACTIVE':
        return [
          { label: 'Bloquear', target: 'BLOCKED', severity: 'danger', confirm: true },
          { label: 'Desativar', target: 'DISABLED', severity: 'secondary', confirm: true },
        ];
      case 'BLOCKED':
        return [{ label: 'Desbloquear', target: 'ACTIVE', severity: 'secondary', confirm: false }];
      case 'DISABLED':
        return [{ label: 'Ativar', target: 'ACTIVE', severity: 'secondary', confirm: false }];
      default:
        return [];
    }
  }

  changeStatus(user: UserResponse, action: StatusAction): void {
    if (!action.confirm) {
      this.updateStatus(user, action.target);
      return;
    }

    this.confirmationService.confirm({
      header: `${action.label} usuário`,
      message:
        action.target === 'BLOCKED'
          ? `Bloquear o usuário ${user.username}? Ele não conseguirá mais autenticar até ser desbloqueado.`
          : `Desativar o usuário ${user.username}? Ele não conseguirá mais autenticar.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: action.label,
      rejectLabel: 'Cancelar',
      accept: () => this.updateStatus(user, action.target),
    });
  }

  resetPassword(user: UserResponse): void {
    this.adminApi.resetUserPassword(this.tenantId, user.id).subscribe({
      next: () => this.messageService.showSuccess(`E-mail de redefinição enviado para ${user.email}.`),
      error: () => this.messageService.showError('Não foi possível iniciar a redefinição de senha.'),
    });
  }

  private updateStatus(user: UserResponse, status: string): void {
    this.adminApi.updateUserStatus(this.tenantId, user.id, { status }).subscribe({
      next: () => {
        this.messageService.showSuccess('Status do usuário atualizado.');
        this.load(this.page());
      },
      error: () => this.messageService.showError('Não foi possível alterar o status do usuário.'),
    });
  }
}
