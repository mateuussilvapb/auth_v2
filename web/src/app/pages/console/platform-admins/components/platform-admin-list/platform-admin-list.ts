//Angular
import { Component, OnInit, inject, signal } from '@angular/core';

//Aplicação
import { ListBase } from '../../../../../shared/components/list-base/list-base';
import { StatusTag } from '../../../../../shared/components/status-tag/status-tag';
import { LayoutBasePages } from '../../../../../shared/components/layout-base-pages/layout-base-pages';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { PlatformAdminResponse } from '../../../../../core/models/admin-api.models';

//Externos
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule, TablePageEvent } from 'primeng/table';

const PAGE_SIZE = 10;

/**
 * Rota {@code /console/platform-admins} (guia de estilo, seção 5.3) — item que fechava a
 * lacuna do plano (seção 5, fase 7): a admin-api expõe o endpoint desde a Fase 8 do backend,
 * mas nenhuma tela do console o consumia até aqui. Sem edição (a admin-api não tem `PUT`
 * nem `GET :id` para platform admin) — só criação e alternância de status.
 */
@Component({
  selector: 'app-platform-admin-list',
  imports: [TableModule, ButtonModule, SkeletonModule, StatusTag, LayoutBasePages],
  templateUrl: './platform-admin-list.html',
})
export class PlatformAdminList extends ListBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);

  admins = signal<PlatformAdminResponse[]>([]);
  totalRecords = signal(0);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  page = signal(0);
  readonly pageSize = PAGE_SIZE;
  readonly skeletonRows = Array.from({ length: 5 }, (_, i) => i);

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    this.page.set(page);
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminApi.listPlatformAdmins(page, this.pageSize).subscribe({
      next: (result) => {
        this.admins.set(result.content);
        this.totalRecords.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os platform admins.');
        this.loading.set(false);
      },
    });
  }

  onPage(event: TablePageEvent): void {
    this.load(Math.floor(event.first / event.rows));
  }

  goToCreate(): void {
    this.router.navigate(['/console/platform-admins/novo']);
  }

  toggleStatus(admin: PlatformAdminResponse): void {
    if (admin.status === 'ACTIVE') {
      this.confirmationService.confirm({
        header: 'Desativar platform admin',
        message: `Desativar o platform admin ${admin.username}? Ele não conseguirá mais acessar o console.`,
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Desativar',
        rejectLabel: 'Cancelar',
        accept: () => this.updateStatus(admin, 'INACTIVE'),
      });
      return;
    }

    this.updateStatus(admin, 'ACTIVE');
  }

  private updateStatus(admin: PlatformAdminResponse, status: string): void {
    this.adminApi.updatePlatformAdminStatus(admin.id, { status }).subscribe({
      next: () => {
        this.messageService.showSuccess(status === 'ACTIVE' ? 'Platform admin ativado.' : 'Platform admin desativado.');
        this.load(this.page());
      },
      error: () => this.messageService.showError('Não foi possível alterar o status do platform admin.'),
    });
  }
}
