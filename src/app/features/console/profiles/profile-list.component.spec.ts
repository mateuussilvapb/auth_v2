import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import type { Mock } from 'vitest';

import { ProfileListComponent } from './profile-list.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { SystemProfileResponse } from '../../../core/models/admin-api.models';

describe('ProfileListComponent', () => {
  let fixture: ComponentFixture<ProfileListComponent>;
  let adminApiStub: { listProfiles: Mock; createProfile: Mock; updateProfileStatus: Mock };

  const profiles: SystemProfileResponse[] = [
    { id: '1', systemId: '1', code: 'ADMIN', description: 'Administrador', status: 'ACTIVE' },
  ];

  beforeEach(async () => {
    adminApiStub = {
      listProfiles: vi.fn().mockReturnValue(of(profiles)),
      createProfile: vi.fn().mockReturnValue(of(profiles[0])),
      updateProfileStatus: vi.fn().mockReturnValue(of(profiles[0])),
    };

    await TestBed.configureTestingModule({
      imports: [ProfileListComponent],
      providers: [
        provideRouter([]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ systemId: '1' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileListComponent);
    fixture.detectChanges();
  });

  it('deve carregar perfis do systemId da rota', () => {
    expect(adminApiStub.listProfiles).toHaveBeenCalledWith('1');
    expect(fixture.componentInstance.profiles()).toEqual(profiles);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listProfiles.mockReturnValue(throwError(() => new Error('falhou')));
    fixture.componentInstance.load();
    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('deve criar perfil e limpar o formulário', () => {
    fixture.componentInstance.newCode = 'OPERADOR';
    fixture.componentInstance.newDescription = 'Operador';
    fixture.componentInstance.create();

    expect(adminApiStub.createProfile).toHaveBeenCalledWith('1', { code: 'OPERADOR', description: 'Operador' });
    expect(fixture.componentInstance.newCode).toBe('');
  });

  it('deve chamar updateProfileStatus com o status invertido', () => {
    fixture.componentInstance.toggleStatus(profiles[0]);
    expect(adminApiStub.updateProfileStatus).toHaveBeenCalledWith('1', '1', { status: 'INACTIVE' });
  });
});
