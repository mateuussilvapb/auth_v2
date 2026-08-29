//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

//Aplicação
import { FormBase } from '../../../../../shared/components/form-base/form-base';
import { FormLabel } from '../../../../../shared/components/form-label/form-label';
import { CopyField } from '../../../../../shared/components/copy-field/copy-field';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { ApiErrorResponse } from '../../../../../core/models/auth-api.models';
import { SystemResponse } from '../../../../../core/models/admin-api.models';
import { generateClientSecret } from '../../../../../core/util/secret';

//Externos
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';

/**
 * Rota {@code /console/tenants/:tenantId/systems/novo} (criação, página cheia) e diálogo de
 * edição aberto por {@code SystemList} (guia de estilo, seção 5.2/7.2). Edição usa diálogo em
 * vez de rota porque a admin-api **não tem** `GET /admin/api/v1/systems/{id}` — só
 * `GET .../tenants/{tenantId}/systems` (lista). `SystemList` já tem o `SystemResponse`
 * completo em memória ao abrir "Editar", então passa via `dialogData.valoresIniciais` em vez
 * de a tela precisar buscar de novo.
 * <p>
 * Só {@code name} é editável depois da criação (`UpdateSystemRequest` do backend não aceita
 * mais nada) — `clientId`/`publicClient`/`thirdParty` ficam desabilitados em modo edição
 * (visíveis, não escondidos, guia 5.2). Redirect URIs e rotação de secret são ações
 * independentes do submit do formulário, disponíveis só em modo edição.
 * </p>
 * <p>
 * {@code clientSecret} nunca é digitado pelo admin (guia 5.5) — é gerado no navegador
 * ({@link generateClientSecret}) tanto na criação quanto na rotação, e exibido uma única vez
 * via {@link CopyField}.
 * </p>
 */
@Component({
  selector: 'app-system-form',
  imports: [ReactiveFormsModule, ButtonModule, CheckboxModule, InputTextModule, FormLabel, CopyField],
  templateUrl: './system-form.html',
})
export class SystemForm extends FormBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);

  tenantId = '';
  redirectUris = signal<string[]>([]);
  newRedirectUri = signal('');
  generatedSecret = signal<string | null>(null);
  rotatedSecret = signal<string | null>(null);

  ngOnInit(): void {
    this.form = this.fb.group({
      clientId: this.fb.control('', { validators: [Validators.required] }),
      name: this.fb.control('', { validators: [Validators.required] }),
      publicClient: this.fb.control(true),
      thirdParty: this.fb.control(false),
    });

    if (this.isEditMode()) {
      const initial = this.dialogData()?.valoresIniciais as SystemResponse | undefined;
      if (initial) {
        this.form.patchValue({
          clientId: initial.clientId,
          name: initial.name,
          publicClient: initial.publicClient,
          thirdParty: initial.thirdParty,
        });
        this.redirectUris.set(initial.redirectUris);
      }
      this.form.get('clientId')?.disable();
      this.form.get('publicClient')?.disable();
      this.form.get('thirdParty')?.disable();
      return;
    }

    this.tenantId = this.route.snapshot.paramMap.get('tenantId') ?? '';
    this.redirectUris.set(['']);
  }

  onPublicClientChange(): void {
    this.generatedSecret.set(this.form.get('publicClient')?.value ? null : generateClientSecret());
  }

  addRedirectUriField(): void {
    this.redirectUris.update((uris) => [...uris, '']);
  }

  updateRedirectUriField(index: number, value: string): void {
    this.redirectUris.update((uris) => uris.map((uri, i) => (i === index ? value : uri)));
  }

  removeRedirectUriField(index: number): void {
    this.redirectUris.update((uris) => uris.filter((_, i) => i !== index));
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { clientId, name, publicClient, thirdParty } = this.form.getRawValue();

    if (this.isEditMode()) {
      this.submitting.set(true);
      try {
        await firstValueFrom(this.adminApi.updateSystem(this.pageId(), { name: name! }));
        this.messageService.showSuccess('Sistema atualizado.');
        this.finalizar(true, []);
      } catch (error) {
        const apiError = error as { error?: ApiErrorResponse };
        this.messageService.showError(apiError.error?.message ?? 'Não foi possível salvar o sistema.');
      } finally {
        this.submitting.set(false);
      }
      return;
    }

    const initialRedirectUris = this.redirectUris()
      .map((uri) => uri.trim())
      .filter((uri) => uri.length > 0);

    if (initialRedirectUris.length === 0) {
      this.messageService.showError('Informe ao menos uma redirect URI.');
      return;
    }

    this.submitting.set(true);
    try {
      await firstValueFrom(
        this.adminApi.createSystem(this.tenantId, {
          clientId: clientId!,
          name: name!,
          publicClient: publicClient!,
          clientSecret: publicClient ? null : this.generatedSecret(),
          initialRedirectUris,
          thirdParty: thirdParty!,
        }),
      );
      this.messageService.showSuccess('Sistema criado.');
      this.finalizar(undefined, ['/console/tenants', this.tenantId, 'systems']);
    } catch (error) {
      const apiError = error as { error?: ApiErrorResponse };
      this.messageService.showError(apiError.error?.message ?? 'Não foi possível criar o sistema.');
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    if (this.isEditMode()) {
      this.finalizar(undefined, []);
      return;
    }
    this.finalizar(undefined, ['/console/tenants', this.tenantId, 'systems']);
  }

  async addRedirectUri(): Promise<void> {
    const uri = this.newRedirectUri().trim();
    if (!uri) {
      return;
    }

    try {
      const updated = await firstValueFrom(this.adminApi.addRedirectUri(this.pageId(), { uri }));
      this.redirectUris.set(updated.redirectUris);
      this.newRedirectUri.set('');
      this.messageService.showSuccess('Redirect URI adicionada.');
    } catch {
      this.messageService.showError('Não foi possível adicionar a redirect URI.');
    }
  }

  removeRedirectUri(uri: string): void {
    this.confirmationService.confirm({
      header: 'Remover redirect URI',
      message: `Remover a redirect URI ${uri}? Integrações que dependem dela deixarão de autenticar.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Remover',
      rejectLabel: 'Cancelar',
      accept: () => this.doRemoveRedirectUri(uri),
    });
  }

  private async doRemoveRedirectUri(uri: string): Promise<void> {
    try {
      const updated = await firstValueFrom(this.adminApi.removeRedirectUri(this.pageId(), uri));
      this.redirectUris.set(updated.redirectUris);
      this.messageService.showSuccess('Redirect URI removida.');
    } catch {
      this.messageService.showError('Não foi possível remover a redirect URI.');
    }
  }

  rotateSecret(): void {
    const name = this.form.getRawValue().name;
    this.confirmationService.confirm({
      header: 'Rotacionar secret',
      message: `Rotacionar o secret do sistema ${name}? O secret atual deixará de funcionar imediatamente.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Rotacionar',
      rejectLabel: 'Cancelar',
      accept: () => this.doRotateSecret(),
    });
  }

  private async doRotateSecret(): Promise<void> {
    try {
      const newSecret = generateClientSecret();
      await firstValueFrom(this.adminApi.rotateSecret(this.pageId(), { newSecret }));
      this.rotatedSecret.set(newSecret);
      this.messageService.showSuccess('Secret rotacionado.');
    } catch {
      this.messageService.showError('Não foi possível rotacionar o secret.');
    }
  }
}
