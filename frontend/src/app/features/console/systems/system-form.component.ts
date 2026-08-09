import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AdminApiService } from '../../../core/services/admin-api.service';
import { ApiErrorResponse } from '../../../core/models/auth-api.models';

/** Rota {@code /console/tenants/:tenantId/systems/novo} — só criação (sem edição de client_id/tipo). */
@Component({
  selector: 'app-system-form',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './system-form.component.html',
})
export class SystemFormComponent {
  tenantId: number;
  clientId = '';
  name = '';
  publicClient = true;
  clientSecret = '';
  thirdParty = false;
  redirectUris: string[] = [''];

  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly adminApi: AdminApiService,
  ) {
    this.tenantId = Number(this.route.snapshot.paramMap.get('tenantId'));
  }

  addRedirectUriField(): void {
    this.redirectUris.push('');
  }

  removeRedirectUriField(index: number): void {
    this.redirectUris.splice(index, 1);
  }

  submit(): void {
    this.errorMessage.set(null);
    this.submitting.set(true);

    const initialRedirectUris = this.redirectUris.map((uri) => uri.trim()).filter((uri) => uri.length > 0);

    this.adminApi
      .createSystem(this.tenantId, {
        clientId: this.clientId,
        name: this.name,
        publicClient: this.publicClient,
        clientSecret: this.publicClient ? null : this.clientSecret,
        initialRedirectUris,
        thirdParty: this.thirdParty,
      })
      .subscribe({
        next: () => this.router.navigate(['/console/tenants', this.tenantId, 'systems']),
        error: (error: { error?: ApiErrorResponse }) => {
          this.submitting.set(false);
          this.errorMessage.set(error.error?.message ?? 'Não foi possível criar o sistema.');
        },
      });
  }
}
