//Angular
import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

//Aplicação
import { FormBase } from '../../../../../shared/components/form-base/form-base';
import { FormLabel } from '../../../../../shared/components/form-label/form-label';
import { LoadingOverlayService } from '../../../../../shared/services/loading-overlay.service';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { ApiErrorResponse } from '../../../../../core/models/auth-api.models';

//Externos
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

/**
 * Rotas {@code /console/tenants/novo} e {@code /console/tenants/:id/editar} (guia de
 * estilo, seção 5.2/7.2) — {@code code} é imutável após a criação (seção 3.2 do plano), então
 * o campo fica desabilitado (não escondido, seção 5.2) em modo edição.
 */
@Component({
  selector: 'app-tenant-form',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, FormLabel],
  templateUrl: './tenant-form.html',
})
export class TenantForm extends FormBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly loadingOverlay = inject(LoadingOverlayService);

  ngOnInit(): void {
    this.form = this.fb.group({
      code: this.fb.control('', { validators: [Validators.required] }),
      name: this.fb.control('', { validators: [Validators.required] }),
    });

    if (this.isEditMode()) {
      this.form.get('code')?.disable();
      this.load();
    }
  }

  private async load(): Promise<void> {
    try {
      await this.loadingOverlay.wrap(async () => {
        const tenant = await firstValueFrom(this.adminApi.getTenant(this.pageId()));
        this.form.patchValue({ code: tenant.code, name: tenant.name });
      });
    } catch {
      this.messageService.showError('Não foi possível carregar o tenant.');
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { code, name } = this.form.getRawValue();

    try {
      if (this.isEditMode()) {
        await firstValueFrom(this.adminApi.updateTenant(this.pageId(), { name: name! }));
      } else {
        await firstValueFrom(this.adminApi.createTenant({ code: code!, name: name! }));
      }
      this.messageService.showSuccess(this.isEditMode() ? 'Tenant atualizado.' : 'Tenant criado.');
      this.finalizar(undefined, ['/console/tenants']);
    } catch (error) {
      const apiError = error as { error?: ApiErrorResponse };
      this.messageService.showError(apiError.error?.message ?? 'Não foi possível salvar o tenant.');
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.finalizar(undefined, ['/console/tenants']);
  }
}
