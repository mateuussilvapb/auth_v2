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
import { PasswordModule } from 'primeng/password';

/**
 * Rotas {@code /console/tenants/:tenantId/users/novo} e
 * {@code /console/tenants/:tenantId/users/:id/editar} (guia de estilo, seção 5.2/7.2).
 * {@code username} é imutável após a criação (só {@code name}/{@code email} entram em
 * {@code UpdateUserRequest}) — fica desabilitado (não escondido, guia 5.2) em modo edição.
 * A senha nunca é editada por aqui, só via "Redefinir senha" em {@code UserList} (fluxo de
 * e-mail, seção 7.4) — o campo de senha inicial só existe em modo criação.
 */
@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, PasswordModule, FormLabel],
  templateUrl: './user-form.html',
})
export class UserForm extends FormBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly loadingOverlay = inject(LoadingOverlayService);

  tenantId = '';

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.paramMap.get('tenantId') ?? '';

    if (this.isEditMode()) {
      this.form = this.fb.group({
        username: this.fb.control({ value: '', disabled: true }),
        name: this.fb.control('', { validators: [Validators.required] }),
        email: this.fb.control('', { validators: [Validators.required, Validators.email] }),
      });
      this.load();
      return;
    }

    this.form = this.fb.group({
      username: this.fb.control('', { validators: [Validators.required] }),
      name: this.fb.control('', { validators: [Validators.required] }),
      email: this.fb.control('', { validators: [Validators.required, Validators.email] }),
      password: this.fb.control('', { validators: [Validators.required, Validators.minLength(8)] }),
    });
  }

  private async load(): Promise<void> {
    try {
      await this.loadingOverlay.wrap(async () => {
        const user = await firstValueFrom(this.adminApi.getUser(this.tenantId, this.pageId()));
        this.form.patchValue({ username: user.username, name: user.name, email: user.email });
      });
    } catch {
      this.messageService.showError('Não foi possível carregar o usuário.');
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const raw = this.form.getRawValue();

    try {
      if (this.isEditMode()) {
        await firstValueFrom(this.adminApi.updateUser(this.tenantId, this.pageId(), { name: raw.name!, email: raw.email! }));
        this.messageService.showSuccess('Usuário atualizado.');
      } else {
        await firstValueFrom(
          this.adminApi.createUser(this.tenantId, {
            username: raw.username!,
            email: raw.email!,
            password: raw.password!,
            name: raw.name!,
          }),
        );
        this.messageService.showSuccess('Usuário criado.');
      }
      this.finalizar(undefined, ['/console/tenants', this.tenantId, 'users']);
    } catch (error) {
      const apiError = error as { error?: ApiErrorResponse };
      this.messageService.showError(apiError.error?.message ?? 'Não foi possível salvar o usuário.');
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.finalizar(undefined, ['/console/tenants', this.tenantId, 'users']);
  }
}
