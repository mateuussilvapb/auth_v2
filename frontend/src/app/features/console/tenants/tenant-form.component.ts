import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AdminApiService } from '../../../core/services/admin-api.service';
import { ApiErrorResponse } from '../../../core/models/auth-api.models';

/**
 * Rotas {@code /console/tenants/novo} e {@code /console/tenants/:id/editar} — o
 * {@code code} do tenant é imutável após a criação (seção 3.2 do plano), então o campo só
 * aparece (e é editável) na criação.
 */
@Component({
  selector: 'app-tenant-form',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './tenant-form.component.html',
})
export class TenantFormComponent implements OnInit {
  tenantId: number | null = null;
  code = '';
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
    return this.tenantId !== null;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    this.tenantId = Number(idParam);
    this.loading.set(true);
    this.adminApi.getTenant(this.tenantId).subscribe({
      next: (tenant) => {
        this.code = tenant.code;
        this.name = tenant.name;
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar o tenant.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    this.errorMessage.set(null);
    this.submitting.set(true);

    const request$ = this.isEditing
      ? this.adminApi.updateTenant(this.tenantId!, { name: this.name })
      : this.adminApi.createTenant({ code: this.code, name: this.name });

    request$.subscribe({
      next: () => this.router.navigate(['/console/tenants']),
      error: (error: { error?: ApiErrorResponse }) => {
        this.submitting.set(false);
        this.errorMessage.set(error.error?.message ?? 'Não foi possível salvar o tenant.');
      },
    });
  }
}
