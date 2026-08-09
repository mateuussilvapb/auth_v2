import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { AdminApiService } from '../../../core/services/admin-api.service';
import { SystemProfileResponse } from '../../../core/models/admin-api.models';

/**
 * Rota {@code /console/systems/:systemId/profiles} — {@code SystemProfile.code} é
 * {@code UNIQUE (systemId, code)} (seção 3.2 do plano), então a listagem é sempre por
 * sistema, sem paginação (poucos perfis por sistema na prática — o backend também não
 * pagina esse endpoint).
 */
@Component({
  selector: 'app-profile-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-list.component.html',
})
export class ProfileListComponent implements OnInit {
  systemId = '';
  profiles = signal<SystemProfileResponse[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);

  newCode = '';
  newDescription = '';
  submitting = signal(false);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly adminApi: AdminApiService,
  ) {}

  ngOnInit(): void {
    this.systemId = this.route.snapshot.paramMap.get('systemId') ?? '';
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminApi.listProfiles(this.systemId).subscribe({
      next: (profiles) => {
        this.profiles.set(profiles);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar os perfis.');
        this.loading.set(false);
      },
    });
  }

  create(): void {
    this.submitting.set(true);
    this.adminApi.createProfile(this.systemId, { code: this.newCode, description: this.newDescription || null }).subscribe({
      next: () => {
        this.newCode = '';
        this.newDescription = '';
        this.submitting.set(false);
        this.load();
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set('Não foi possível criar o perfil.');
      },
    });
  }

  toggleStatus(profile: SystemProfileResponse): void {
    const newStatus = profile.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.adminApi.updateProfileStatus(this.systemId, profile.id, { status: newStatus }).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Não foi possível alterar o status do perfil.'),
    });
  }
}
