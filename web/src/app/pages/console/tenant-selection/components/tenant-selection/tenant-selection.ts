//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

//Aplicação
import { ListBase } from '../../../../../shared/components/list-base/list-base';
import { StatusTag } from '../../../../../shared/components/status-tag/status-tag';
import { LayoutBasePages } from '../../../../../shared/components/layout-base-pages/layout-base-pages';
import { LoadingOverlayService } from '../../../../../shared/services/loading-overlay.service';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { ConsoleAuthService } from '../../../../../core/services/console-auth.service';
import { TenantContextService } from '../../../../../core/services/tenant-context.service';
import { TenantResponse } from '../../../../../core/models/admin-api.models';

//Externos
import { ButtonModule } from 'primeng/button';
import { TableModule, TablePageEvent } from 'primeng/table';

const PAGE_SIZE = 10;

/**
 * Rota {@code /console/selecionar-tenant} — obrigatória a todo platform admin antes de
 * qualquer tela que não seja gestão de tenants (decisão de produto 2026-08-29, guard
 * `tenantContextGuard`). Selecionar grava o contexto cifrado em localStorage
 * ({@link TenantContextService}) e retoma a navegação original (`returnUrl`) ou vai ao
 * dashboard.
 */
@Component({
  selector: 'app-tenant-selection',
  imports: [TableModule, ButtonModule, StatusTag, LayoutBasePages],
  templateUrl: './tenant-selection.html',
})
export class TenantSelection extends ListBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly consoleAuth = inject(ConsoleAuthService);
  private readonly tenantContext = inject(TenantContextService);
  private readonly loadingOverlay = inject(LoadingOverlayService);
  private readonly route = inject(ActivatedRoute);

  tenants = signal<TenantResponse[]>([]);
  totalRecords = signal(0);
  loading = signal(true);
  readonly pageSize = PAGE_SIZE;

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.adminApi.listTenants(page, PAGE_SIZE).subscribe({
      next: (result) => {
        this.tenants.set(result.content);
        this.totalRecords.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.messageService.showError('Não foi possível carregar os tenants.');
        this.loading.set(false);
      },
    });
  }

  onPage(event: TablePageEvent): void {
    this.load(Math.floor(event.first / event.rows));
  }

  async select(tenant: TenantResponse): Promise<void> {
    await this.loadingOverlay.wrap(async () => {
      await this.tenantContext.select({ id: tenant.id, code: tenant.code }, this.consoleAuth.getAccessToken());
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
      await this.router.navigateByUrl(returnUrl && returnUrl !== '/console/selecionar-tenant' ? returnUrl : '/console');
    });
  }
}
