//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';

//Aplicação
import { ListBase } from '../../../../../shared/components/list-base/list-base';
import { StatusTag } from '../../../../../shared/components/status-tag/status-tag';
import { LayoutBasePages } from '../../../../../shared/components/layout-base-pages/layout-base-pages';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import {
  SystemProfileResponse,
  SystemResponse,
  UserSystemProfileResponse,
  UserSystemResponse,
} from '../../../../../core/models/admin-api.models';

//Externos
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule, TableRowCollapseEvent, TableRowExpandEvent } from 'primeng/table';

interface BindingStatusAction {
  label: string;
  target: string;
  severity: 'secondary' | 'danger';
  confirm: boolean;
}

function statusActionsFor(status: string): BindingStatusAction[] {
  switch (status) {
    case 'ACTIVE':
      return [
        { label: 'Bloquear', target: 'BLOCKED', severity: 'danger', confirm: true },
        { label: 'Desativar', target: 'INACTIVE', severity: 'secondary', confirm: true },
      ];
    case 'BLOCKED':
      return [{ label: 'Desbloquear', target: 'ACTIVE', severity: 'secondary', confirm: false }];
    case 'INACTIVE':
      return [{ label: 'Ativar', target: 'ACTIVE', severity: 'secondary', confirm: false }];
    default:
      return [];
  }
}

/**
 * Rota {@code /console/tenants/:tenantId/users/:userId/bindings} (guia de estilo, seção 5.3)
 * — última tela do checklist de CRUDs. Cada {@link UserSystemResponse} (vínculo
 * usuário → sistema) é uma linha expansível (`p-table` row expansion) com os perfis
 * vinculados a ele dentro — carregados em paralelo no load inicial (poucos sistemas por
 * tenant e poucos perfis por sistema na prática, mesmo racional de {@code ProfileList} para
 * não paginar).
 * <p>
 * {@code BindingStatus} (vínculo usuário↔sistema e vínculo↔perfil) tem os mesmos 3 valores
 * de {@code UserStatus} só que com {@code INACTIVE} em vez de {@code DISABLED}
 * (`BindingStatus.java`) — mesmo padrão de transições de {@code UserList}: ACTIVE oferece
 * Bloquear/Desativar com confirmação, BLOCKED/INACTIVE oferecem só a reativação, sem
 * confirmação.
 * </p>
 */
@Component({
  selector: 'app-user-bindings',
  imports: [FormsModule, TableModule, ButtonModule, SelectModule, SkeletonModule, StatusTag, LayoutBasePages],
  templateUrl: './user-bindings.html',
})
export class UserBindings extends ListBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly activatedRoute = inject(ActivatedRoute);

  tenantId = '';
  userId = '';

  availableSystems = signal<SystemResponse[]>([]);
  bindings = signal<UserSystemResponse[]>([]);
  profileBindingsByUserSystem: Record<string, UserSystemProfileResponse[]> = {};
  profilesBySystem: Record<string, SystemProfileResponse[]> = {};

  loading = signal(true);
  errorMessage = signal<string | null>(null);
  expandedRowKeys = signal<Record<string, boolean>>({});

  newSystemId: string | null = null;
  newProfileId: Record<string, string | null> = {};

  ngOnInit(): void {
    this.tenantId = this.activatedRoute.snapshot.paramMap.get('tenantId') ?? '';
    this.userId = this.activatedRoute.snapshot.paramMap.get('userId') ?? '';
    this.load();
  }

  systemName(systemId: string): string {
    const system = this.availableSystems().find((s) => s.id === systemId);
    return system ? `${system.name} (${system.clientId})` : `Sistema ${systemId}`;
  }

  profileCode(systemId: string, profileId: string): string {
    const profile = (this.profilesBySystem[systemId] ?? []).find((p) => p.id === profileId);
    return profile ? profile.code : `Perfil ${profileId}`;
  }

  availableSystemsToBind(): SystemResponse[] {
    const boundIds = new Set(this.bindings().map((b) => b.systemId));
    return this.availableSystems().filter((s) => !boundIds.has(s.id));
  }

  availableProfilesToBind(binding: UserSystemResponse): SystemProfileResponse[] {
    const boundIds = new Set((this.profileBindingsByUserSystem[binding.id] ?? []).map((p) => p.systemProfileId));
    return (this.profilesBySystem[binding.systemId] ?? []).filter((p) => !boundIds.has(p.id));
  }

  systemStatusActions(binding: UserSystemResponse): BindingStatusAction[] {
    return statusActionsFor(binding.status);
  }

  profileStatusActions(profileBinding: UserSystemProfileResponse): BindingStatusAction[] {
    return statusActionsFor(profileBinding.status);
  }

  onRowExpand(event: TableRowExpandEvent): void {
    const binding = event.data as UserSystemResponse;
    this.expandedRowKeys.update((keys) => ({ ...keys, [binding.id]: true }));
  }

  onRowCollapse(event: TableRowCollapseEvent): void {
    const binding = event.data as UserSystemResponse;
    this.expandedRowKeys.update((keys) => {
      const next = { ...keys };
      delete next[binding.id];
      return next;
    });
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      systems: this.adminApi.listSystemsByTenant(this.tenantId, 0, 100),
      bindings: this.adminApi.listUserSystems(this.tenantId, this.userId, 0, 100),
    }).subscribe({
      next: ({ systems, bindings }) => {
        this.availableSystems.set(systems.content);
        this.bindings.set(bindings.content);
        this.loadProfileData(bindings.content, systems.content);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os vínculos.');
        this.loading.set(false);
      },
    });
  }

  private loadProfileData(bindings: UserSystemResponse[], systems: SystemResponse[]): void {
    if (bindings.length === 0) {
      this.loading.set(false);
      return;
    }

    const profileBindingCalls = bindings.reduce<Record<string, ReturnType<AdminApiService['listUserSystemProfiles']>>>(
      (acc, binding) => {
        acc[binding.id] = this.adminApi.listUserSystemProfiles(this.tenantId, binding.id);
        return acc;
      },
      {},
    );
    const systemIds = [...new Set(bindings.map((b) => b.systemId))];
    const profileCalls = systemIds.reduce<Record<string, ReturnType<AdminApiService['listProfiles']>>>((acc, systemId) => {
      acc[systemId] = this.adminApi.listProfiles(systemId);
      return acc;
    }, {});

    forkJoin({ profileBindings: forkJoin(profileBindingCalls), profiles: forkJoin(profileCalls) }).subscribe({
      next: ({ profileBindings, profiles }) => {
        this.profileBindingsByUserSystem = profileBindings as Record<string, UserSystemProfileResponse[]>;
        this.profilesBySystem = profiles as Record<string, SystemProfileResponse[]>;
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os perfis vinculados.');
        this.loading.set(false);
      },
    });
  }

  bindToSystem(): void {
    if (this.newSystemId === null) {
      return;
    }
    this.adminApi.bindUserToSystem(this.tenantId, this.userId, { systemId: this.newSystemId }).subscribe({
      next: () => {
        this.newSystemId = null;
        this.messageService.showSuccess('Sistema vinculado.');
        this.load();
      },
      error: () => this.messageService.showError('Não foi possível vincular o usuário a este sistema.'),
    });
  }

  changeSystemBindingStatus(binding: UserSystemResponse, action: BindingStatusAction): void {
    if (!action.confirm) {
      this.doChangeSystemBindingStatus(binding, action.target);
      return;
    }

    this.confirmationService.confirm({
      header: `${action.label} vínculo`,
      message:
        action.target === 'BLOCKED'
          ? `Bloquear o acesso do usuário a ${this.systemName(binding.systemId)}? Ele não conseguirá mais autenticar neste sistema até ser desbloqueado.`
          : `Desativar o acesso do usuário a ${this.systemName(binding.systemId)}? Ele não conseguirá mais autenticar neste sistema.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: action.label,
      rejectLabel: 'Cancelar',
      accept: () => this.doChangeSystemBindingStatus(binding, action.target),
    });
  }

  private doChangeSystemBindingStatus(binding: UserSystemResponse, status: string): void {
    this.adminApi.updateUserSystemStatus(this.tenantId, binding.id, { status }).subscribe({
      next: () => {
        this.messageService.showSuccess('Status do vínculo atualizado.');
        this.load();
      },
      error: () => this.messageService.showError('Não foi possível alterar o status do vínculo.'),
    });
  }

  bindProfile(binding: UserSystemResponse): void {
    const profileId = this.newProfileId[binding.id];
    if (!profileId) {
      return;
    }
    this.adminApi.bindProfileToUserSystem(this.tenantId, binding.id, { profileId }).subscribe({
      next: () => {
        this.newProfileId[binding.id] = null;
        this.messageService.showSuccess('Perfil vinculado.');
        this.load();
      },
      error: () => this.messageService.showError('Não foi possível vincular este perfil.'),
    });
  }

  changeProfileBindingStatus(
    binding: UserSystemResponse,
    profileBinding: UserSystemProfileResponse,
    action: BindingStatusAction,
  ): void {
    const profileCode = this.profileCode(binding.systemId, profileBinding.systemProfileId);

    if (!action.confirm) {
      this.doChangeProfileBindingStatus(binding, profileBinding, action.target);
      return;
    }

    this.confirmationService.confirm({
      header: `${action.label} perfil`,
      message:
        action.target === 'BLOCKED'
          ? `Bloquear o perfil ${profileCode} deste usuário? As permissões concedidas por ele deixarão de valer até ser desbloqueado.`
          : `Desativar o perfil ${profileCode} deste usuário? As permissões concedidas por ele deixarão de valer.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: action.label,
      rejectLabel: 'Cancelar',
      accept: () => this.doChangeProfileBindingStatus(binding, profileBinding, action.target),
    });
  }

  private doChangeProfileBindingStatus(
    binding: UserSystemResponse,
    profileBinding: UserSystemProfileResponse,
    status: string,
  ): void {
    this.adminApi.updateUserSystemProfileStatus(this.tenantId, binding.id, profileBinding.id, { status }).subscribe({
      next: () => {
        this.messageService.showSuccess('Status do vínculo de perfil atualizado.');
        this.load();
      },
      error: () => this.messageService.showError('Não foi possível alterar o status do vínculo de perfil.'),
    });
  }
}
