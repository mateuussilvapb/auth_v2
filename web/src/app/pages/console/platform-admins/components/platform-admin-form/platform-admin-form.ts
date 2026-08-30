//Angular
import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

//Aplicação
import { FormBase } from '../../../../../shared/components/form-base/form-base';
import { FormLabel } from '../../../../../shared/components/form-label/form-label';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { ApiErrorResponse } from '../../../../../core/models/auth-api.models';

//Externos
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

/**
 * Rota {@code /console/platform-admins/novo} (guia de estilo, seção 5.2/7.2) — só criação.
 * A admin-api não expõe `PUT`/`GET :id` para platform admin (`PlatformAdminController`), só
 * `POST`, `GET` (lista) e `PATCH .../status` — não há o que editar além do status, que muda
 * direto na listagem.
 */
@Component({
  selector: 'app-platform-admin-form',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, PasswordModule, FormLabel],
  templateUrl: './platform-admin-form.html',
})
export class PlatformAdminForm extends FormBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);

  ngOnInit(): void {
    this.form = this.fb.group({
      username: this.fb.control('', { validators: [Validators.required] }),
      name: this.fb.control('', { validators: [Validators.required] }),
      email: this.fb.control('', { validators: [Validators.required, Validators.email] }),
      password: this.fb.control('', { validators: [Validators.required, Validators.minLength(8)] }),
    });
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { username, name, email, password } = this.form.getRawValue();

    try {
      await firstValueFrom(this.adminApi.createPlatformAdmin({ username: username!, email: email!, password: password!, name: name! }));
      this.messageService.showSuccess('Platform admin criado.');
      this.finalizar(undefined, ['/console/platform-admins']);
    } catch (error) {
      const apiError = error as { error?: ApiErrorResponse };
      this.messageService.showError(apiError.error?.message ?? 'Não foi possível criar o platform admin.');
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.finalizar(undefined, ['/console/platform-admins']);
  }
}
