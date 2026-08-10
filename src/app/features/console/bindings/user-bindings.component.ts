
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AdminApiService } from '../../../core/services/admin-api.service';
import {
  SystemProfileResponse,
  SystemResponse,
  UserSystemProfileResponse,
  UserSystemResponse,
} from '../../../core/models/admin-api.models';

/**
 * Rota {@code /console/tenants/:tenantId/users/:userId/bindings} — última tela do
 * checklist de Fase 9 ("vínculos: usuário → sistemas → perfis"). Cada {@link UserSystemResponse}
 * é expandido com os perfis vinculados a ele (carregados em paralelo — poucos sistemas por
 * tenant e poucos perfis por sistema na prática).
 */
@Component({
  selector: 'app-user-bindings',
  imports: [FormsModule],
  templateUrl: './user-bindings.component.html',
})
export class UserBindingsComponent implements OnInit {
  tenantId = '';
  userId = '';

  availableSystems = signal<SystemResponse[]>([]);
  bindings = signal<UserSystemResponse[]>([]);
  profileBindingsByUserSystem: Record<string, UserSystemProfileResponse[]> = {};
  profilesBySystem: Record<string, SystemProfileResponse[]> = {};

  loading = signal(true);
  errorMessage = signal<string | null>(null);

  newSystemId: string | null = null;
  newProfileId: Record<string, string | null> = {};

  readonly systemStatuses = ['ACTIVE', 'INACTIVE', 'BLOCKED'];
  readonly profileStatuses = ['ACTIVE', 'INACTIVE', 'BLOCKED'];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly adminApi: AdminApiService,
  ) {}

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.paramMap.get('tenantId') ?? '';
    this.userId = this.route.snapshot.paramMap.get('userId') ?? '';
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
        this.load();
      },
      error: () => this.errorMessage.set('Não foi possível vincular o usuário a este sistema.'),
    });
  }

  changeSystemBindingStatus(binding: UserSystemResponse, status: string): void {
    this.adminApi.updateUserSystemStatus(this.tenantId, binding.id, { status }).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Não foi possível alterar o status do vínculo.'),
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
        this.load();
      },
      error: () => this.errorMessage.set('Não foi possível vincular este perfil.'),
    });
  }

  changeProfileBindingStatus(binding: UserSystemResponse, profileBinding: UserSystemProfileResponse, status: string): void {
    this.adminApi.updateUserSystemProfileStatus(this.tenantId, binding.id, profileBinding.id, { status }).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Não foi possível alterar o status do vínculo de perfil.'),
    });
  }
}
