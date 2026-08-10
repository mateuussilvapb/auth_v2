
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AdminApiService } from '../../../core/services/admin-api.service';
import { SystemResponse } from '../../../core/models/admin-api.models';

/**
 * Rota {@code /console/tenants/:tenantId/systems} — sistemas pertencem a exatamente um
 * tenant (D3 do plano), então a listagem sempre parte de um tenant já selecionado, nunca
 * é global.
 */
@Component({
  selector: 'app-system-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './system-list.component.html',
})
export class SystemListComponent implements OnInit {
  tenantId = '';
  systems = signal<SystemResponse[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  newRedirectUri: Record<string, string> = {};

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
    this.adminApi.listSystemsByTenant(this.tenantId, 0, 50).subscribe({
      next: (result) => {
        this.systems.set(result.content);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os sistemas.');
        this.loading.set(false);
      },
    });
  }

  toggleStatus(system: SystemResponse): void {
    const newStatus = system.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.adminApi.updateSystemStatus(system.id, { status: newStatus }).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Não foi possível alterar o status do sistema.'),
    });
  }

  addRedirectUri(system: SystemResponse): void {
    const uri = this.newRedirectUri[system.id];
    if (!uri) {
      return;
    }
    this.adminApi.addRedirectUri(system.id, { uri }).subscribe({
      next: () => {
        this.newRedirectUri[system.id] = '';
        this.load();
      },
      error: () => this.errorMessage.set('Não foi possível adicionar a redirect URI.'),
    });
  }
}
