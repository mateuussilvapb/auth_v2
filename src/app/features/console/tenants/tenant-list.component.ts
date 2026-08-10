import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AdminApiService } from '../../../core/services/admin-api.service';
import { TenantResponse } from '../../../core/models/admin-api.models';

/**
 * Rota {@code /console/tenants} — primeira tela de CRUD do console administrativo (Fase 9,
 * item "CRUD de tenants, sistemas, perfis, usuários" do checklist).
 */
@Component({
  selector: 'app-tenant-list',
  imports: [CommonModule, RouterLink],
  templateUrl: './tenant-list.component.html',
})
export class TenantListComponent implements OnInit {
  tenants = signal<TenantResponse[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  page = signal(0);
  totalPages = signal(0);

  constructor(private readonly adminApi: AdminApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminApi.listTenants(this.page(), 20).subscribe({
      next: (result) => {
        this.tenants.set(result.content);
        this.totalPages.set(result.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os tenants.');
        this.loading.set(false);
      },
    });
  }

  toggleStatus(tenant: TenantResponse): void {
    const newStatus = tenant.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.adminApi.updateTenantStatus(tenant.id, { status: newStatus }).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Não foi possível alterar o status do tenant.'),
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
