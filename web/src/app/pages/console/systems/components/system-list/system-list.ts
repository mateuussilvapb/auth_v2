//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

//Aplicação
import { ListBase } from '../../../../../shared/components/list-base/list-base';
import { StatusTag } from '../../../../../shared/components/status-tag/status-tag';
import { LayoutBasePages } from '../../../../../shared/components/layout-base-pages/layout-base-pages';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { SystemResponse } from '../../../../../core/models/admin-api.models';
import { SystemForm } from '../system-form/system-form';

//Externos
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule, TablePageEvent } from 'primeng/table';

const PAGE_SIZE = 10;

/**
 * Rota {@code /console/tenants/:tenantId/systems} (guia de estilo, seção 5.3) — sistemas
 * pertencem a exatamente um tenant (D3 do plano), então a listagem sempre parte de um tenant
 * já selecionado, nunca é global. "Editar" abre {@link SystemForm} como diálogo (ver javadoc
 * do componente) em vez de navegar — a linha já tem o {@code SystemResponse} completo em
 * memória.
 */
@Component({
  selector: 'app-system-list',
  imports: [TableModule, ButtonModule, SkeletonModule, StatusTag, LayoutBasePages],
  templateUrl: './system-list.html',
})
export class SystemList extends ListBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly activatedRoute = inject(ActivatedRoute);

  tenantId = '';
  systems = signal<SystemResponse[]>([]);
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
    this.adminApi.listSystemsByTenant(this.tenantId, page, this.pageSize).subscribe({
      next: (result) => {
        this.systems.set(result.content);
        this.totalRecords.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os sistemas.');
        this.loading.set(false);
      },
    });
  }

  onPage(event: TablePageEvent): void {
    this.load(Math.floor(event.first / event.rows));
  }

  goToCreate(): void {
    this.router.navigate(['/console/tenants', this.tenantId, 'systems', 'novo']);
  }

  goToProfiles(system: SystemResponse): void {
    this.router.navigate(['/console/systems', system.id, 'profiles']);
  }

  edit(system: SystemResponse): void {
    const ref = this.dialogService.open(SystemForm, {
      header: 'Editar sistema',
      width: '36rem',
      data: { mode: 'edicao', id: system.id, valoresIniciais: system },
    });
    ref?.onClose.subscribe((result) => {
      if (result) {
        this.load(this.page());
      }
    });
  }

  toggleStatus(system: SystemResponse): void {
    if (system.status === 'ACTIVE') {
      this.confirmationService.confirm({
        header: 'Desativar sistema',
        message: `Desativar o sistema ${system.name}? Usuários deste sistema não conseguirão mais autenticar.`,
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Desativar',
        rejectLabel: 'Cancelar',
        accept: () => this.updateStatus(system, 'INACTIVE'),
      });
      return;
    }

    this.updateStatus(system, 'ACTIVE');
  }

  private updateStatus(system: SystemResponse, status: string): void {
    this.adminApi.updateSystemStatus(system.id, { status }).subscribe({
      next: () => {
        this.messageService.showSuccess(status === 'ACTIVE' ? 'Sistema ativado.' : 'Sistema desativado.');
        this.load(this.page());
      },
      error: () => this.messageService.showError('Não foi possível alterar o status do sistema.'),
    });
  }
}
