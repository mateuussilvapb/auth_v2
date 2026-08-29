//Angular
import { Component, OnInit, inject, signal } from '@angular/core';

//Aplicação
import { ListBase } from '../../../../../shared/components/list-base/list-base';
import { StatusTag } from '../../../../../shared/components/status-tag/status-tag';
import { LayoutBasePages } from '../../../../../shared/components/layout-base-pages/layout-base-pages';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { TenantResponse } from '../../../../../core/models/admin-api.models';

//Externos
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule, TablePageEvent } from 'primeng/table';

const PAGE_SIZE = 10;

/**
 * Rota {@code /console/tenants} (guia de estilo, seção 5.3) — primeira tela de CRUD
 * migrada para `p-table` + paginação server-side + `StatusTag` + confirmação em ação
 * destrutiva.
 */
@Component({
  selector: 'app-tenant-list',
  imports: [TableModule, ButtonModule, SkeletonModule, StatusTag, LayoutBasePages],
  templateUrl: './tenant-list.html',
})
export class TenantList extends ListBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);

  tenants = signal<TenantResponse[]>([]);
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
    this.adminApi.listTenants(page, this.pageSize).subscribe({
      next: (result) => {
        this.tenants.set(result.content);
        this.totalRecords.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os tenants.');
        this.loading.set(false);
      },
    });
  }

  onPage(event: TablePageEvent): void {
    this.load(Math.floor(event.first / event.rows));
  }

  goToCreate(): void {
    this.router.navigate(['/console/tenants/novo']);
  }

  goToEdit(tenant: TenantResponse): void {
    this.router.navigate(['/console/tenants', tenant.id, 'editar']);
  }

  goToSystems(tenant: TenantResponse): void {
    this.router.navigate(['/console/tenants', tenant.id, 'systems']);
  }

  goToUsers(tenant: TenantResponse): void {
    this.router.navigate(['/console/tenants', tenant.id, 'users']);
  }

  toggleStatus(tenant: TenantResponse): void {
    if (tenant.status === 'ACTIVE') {
      this.confirmationService.confirm({
        header: 'Desativar tenant',
        message: `Desativar o tenant ${tenant.name}? Usuários deste tenant não conseguirão mais autenticar.`,
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Desativar',
        rejectLabel: 'Cancelar',
        accept: () => this.updateStatus(tenant, 'INACTIVE'),
      });
      return;
    }

    this.updateStatus(tenant, 'ACTIVE');
  }

  private updateStatus(tenant: TenantResponse, status: string): void {
    this.adminApi.updateTenantStatus(tenant.id, { status }).subscribe({
      next: () => {
        this.messageService.showSuccess(status === 'ACTIVE' ? 'Tenant ativado.' : 'Tenant desativado.');
        this.load(this.page());
      },
      error: () => this.messageService.showError('Não foi possível alterar o status do tenant.'),
    });
  }
}
