import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ProfileListComponent } from './profile-list.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { SystemProfileResponse } from '../../../core/models/admin-api.models';

describe('ProfileListComponent', () => {
  let fixture: ComponentFixture<ProfileListComponent>;
  let adminApiStub: { listProfiles: jasmine.Spy; createProfile: jasmine.Spy; updateProfileStatus: jasmine.Spy };

  const profiles: SystemProfileResponse[] = [
    { id: '1', systemId: '1', code: 'ADMIN', description: 'Administrador', status: 'ACTIVE' },
  ];

  beforeEach(async () => {
    adminApiStub = {
      listProfiles: jasmine.createSpy('listProfiles').and.returnValue(of(profiles)),
      createProfile: jasmine.createSpy('createProfile').and.returnValue(of(profiles[0])),
      updateProfileStatus: jasmine.createSpy('updateProfileStatus').and.returnValue(of(profiles[0])),
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
    adminApiStub.listProfiles.and.returnValue(throwError(() => new Error('falhou')));
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
