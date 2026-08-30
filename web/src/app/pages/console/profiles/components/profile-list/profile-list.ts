//Angular
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

//Aplicação
import { ListBase } from '../../../../../shared/components/list-base/list-base';
import { StatusTag } from '../../../../../shared/components/status-tag/status-tag';
import { LayoutBasePages } from '../../../../../shared/components/layout-base-pages/layout-base-pages';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { SystemProfileResponse } from '../../../../../core/models/admin-api.models';

//Externos
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

/**
 * Rota {@code /console/systems/:systemId/profiles} (guia de estilo, seção 5.3) —
 * {@code SystemProfile.code} é único por sistema (seção 3.2 do plano), então a listagem é
 * sempre por sistema. Sem paginação: o endpoint (`GET .../profiles`) também não pagina — na
 * prática poucos perfis por sistema.
 */
@Component({
  selector: 'app-profile-list',
  imports: [TableModule, ButtonModule, SkeletonModule, StatusTag, LayoutBasePages],
  templateUrl: './profile-list.html',
})
export class ProfileList extends ListBase implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly activatedRoute = inject(ActivatedRoute);

  systemId = '';
  profiles = signal<SystemProfileResponse[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  readonly skeletonRows = Array.from({ length: 3 }, (_, i) => i);

  ngOnInit(): void {
    this.systemId = this.activatedRoute.snapshot.paramMap.get('systemId') ?? '';
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

  goToCreate(): void {
    this.router.navigate(['/console/systems', this.systemId, 'profiles', 'novo']);
  }

  goToEdit(profile: SystemProfileResponse): void {
    this.router.navigate(['/console/systems', this.systemId, 'profiles', profile.id, 'editar']);
  }

  toggleStatus(profile: SystemProfileResponse): void {
    if (profile.status === 'ACTIVE') {
      this.confirmationService.confirm({
        header: 'Desativar perfil',
        message: `Desativar o perfil ${profile.code}? Vínculos de usuário com esse perfil deixarão de conceder acesso.`,
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Desativar',
        rejectLabel: 'Cancelar',
        accept: () => this.updateStatus(profile, 'INACTIVE'),
      });
      return;
    }

    this.updateStatus(profile, 'ACTIVE');
  }

  private updateStatus(profile: SystemProfileResponse, status: string): void {
    this.adminApi.updateProfileStatus(this.systemId, profile.id, { status }).subscribe({
      next: () => {
        this.messageService.showSuccess(status === 'ACTIVE' ? 'Perfil ativado.' : 'Perfil desativado.');
        this.load();
      },
      error: () => this.messageService.showError('Não foi possível alterar o status do perfil.'),
    });
  }
}
