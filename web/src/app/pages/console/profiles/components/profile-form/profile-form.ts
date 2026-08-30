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
 * Rotas {@code /console/systems/:systemId/profiles/novo} e
 * {@code /console/systems/:systemId/profiles/:id/editar} (guia de estilo, seção 5.2/7.2).
 * {@code code} é único por sistema (`UNIQUE (systemId, code)`, seção 3.2 do plano) e imutável
 * depois da criação — fica desabilitado (não escondido, guia 5.2) em modo edição. Só
 * {@code description} é aceito por {@code UpdateSystemProfileRequest}.
 */
@Component({
  selector: 'app-profile-form',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, FormLabel],
  templateUrl: './profile-form.html',
})
export class ProfileForm extends FormBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly loadingOverlay = inject(LoadingOverlayService);

  systemId = '';

  ngOnInit(): void {
    this.systemId = this.route.snapshot.paramMap.get('systemId') ?? '';

    this.form = this.fb.group({
      code: this.fb.control('', { validators: [Validators.required] }),
      description: this.fb.control(''),
    });

    if (this.isEditMode()) {
      this.form.get('code')?.disable();
      this.load();
    }
  }

  private async load(): Promise<void> {
    try {
      await this.loadingOverlay.wrap(async () => {
        const profile = await firstValueFrom(this.adminApi.getProfile(this.systemId, this.pageId()));
        this.form.patchValue({ code: profile.code, description: profile.description ?? '' });
      });
    } catch {
      this.messageService.showError('Não foi possível carregar o perfil.');
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { code, description } = this.form.getRawValue();

    try {
      if (this.isEditMode()) {
        await firstValueFrom(
          this.adminApi.updateProfile(this.systemId, this.pageId(), { description: description || null }),
        );
        this.messageService.showSuccess('Perfil atualizado.');
      } else {
        await firstValueFrom(this.adminApi.createProfile(this.systemId, { code: code!, description: description || null }));
        this.messageService.showSuccess('Perfil criado.');
      }
      this.finalizar(undefined, ['/console/systems', this.systemId, 'profiles']);
    } catch (error) {
      const apiError = error as { error?: ApiErrorResponse };
      this.messageService.showError(apiError.error?.message ?? 'Não foi possível salvar o perfil.');
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.finalizar(undefined, ['/console/systems', this.systemId, 'profiles']);
  }
}
