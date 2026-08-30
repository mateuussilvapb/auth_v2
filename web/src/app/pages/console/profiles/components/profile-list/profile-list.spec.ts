import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { ProfileList } from './profile-list';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { SystemProfileResponse } from '../../../../../core/models/admin-api.models';

describe('ProfileList', () => {
  let fixture: ComponentFixture<ProfileList>;
  let adminApiStub: { listProfiles: Mock; updateProfileStatus: Mock };
  let router: Router;

  const activeProfile: SystemProfileResponse = {
    id: '1',
    systemId: '1',
    code: 'ADMIN',
    description: 'Administrador',
    status: 'ACTIVE',
  };
  const inactiveProfile: SystemProfileResponse = { ...activeProfile, id: '2', code: 'OPERADOR', status: 'INACTIVE' };

  beforeEach(async () => {
    adminApiStub = {
      listProfiles: vi.fn().mockReturnValue(of([activeProfile])),
      updateProfileStatus: vi.fn().mockReturnValue(of(activeProfile)),
    };

    await TestBed.configureTestingModule({
      imports: [ProfileList],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ systemId: '1' }) } } },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(ProfileList);
    fixture.detectChanges();
  });

  it('deve carregar perfis do systemId da rota', () => {
    expect(fixture.componentInstance.systemId).toBe('1');
    expect(adminApiStub.listProfiles).toHaveBeenCalledWith('1');
    expect(fixture.componentInstance.profiles()).toEqual([activeProfile]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listProfiles.mockReturnValue(throwError(() => new Error('falhou')));

    fixture.componentInstance.load();

    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('deve pedir confirmação antes de desativar um perfil ativo', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.toggleStatus(activeProfile);

    expect(confirmSpy).toHaveBeenCalled();
    expect(adminApiStub.updateProfileStatus).not.toHaveBeenCalled();

    confirmSpy.mock.calls[0][0].accept?.();

    expect(adminApiStub.updateProfileStatus).toHaveBeenCalledWith('1', '1', { status: 'INACTIVE' });
  });

  it('deve ativar um perfil inativo sem pedir confirmação', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.toggleStatus(inactiveProfile);

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(adminApiStub.updateProfileStatus).toHaveBeenCalledWith('1', '2', { status: 'ACTIVE' });
  });

  it('deve navegar para a criação de perfil com o systemId', () => {
    fixture.componentInstance.goToCreate();
    expect(router.navigate).toHaveBeenCalledWith(['/console/systems', '1', 'profiles', 'novo']);
  });

  it('deve navegar para a edição de um perfil', () => {
    fixture.componentInstance.goToEdit(activeProfile);
    expect(router.navigate).toHaveBeenCalledWith(['/console/systems', '1', 'profiles', '1', 'editar']);
  });
});
