import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import type { Mock } from 'vitest';

import { UserBindingsComponent } from './user-bindings.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { Page, SystemProfileResponse, SystemResponse, UserSystemProfileResponse, UserSystemResponse } from '../../../core/models/admin-api.models';

describe('UserBindingsComponent', () => {
  let fixture: ComponentFixture<UserBindingsComponent>;
  let adminApiStub: {
    listSystemsByTenant: Mock;
    listUserSystems: Mock;
    listUserSystemProfiles: Mock;
    listProfiles: Mock;
    bindUserToSystem: Mock;
    updateUserSystemStatus: Mock;
    bindProfileToUserSystem: Mock;
    updateUserSystemProfileStatus: Mock;
  };

  const systemsPage: Page<SystemResponse> = {
    content: [
      { id: '3', clientId: 'CRM_ACME', name: 'CRM', status: 'ACTIVE', publicClient: true, redirectUris: [], thirdParty: false },
    ],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 100,
  };

  const userSystemsPage: Page<UserSystemResponse> = {
    content: [{ id: '10', userId: '2', systemId: '3', tenantId: '1', status: 'ACTIVE' }],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 100,
  };

  const profileBindings: UserSystemProfileResponse[] = [{ id: '20', userSystemId: '10', systemProfileId: '5', status: 'ACTIVE' }];
  const profiles: SystemProfileResponse[] = [{ id: '5', systemId: '3', code: 'ADMIN', description: 'Administrador', status: 'ACTIVE' }];

  beforeEach(async () => {
    adminApiStub = {
      listSystemsByTenant: vi.fn().mockReturnValue(of(systemsPage)),
      listUserSystems: vi.fn().mockReturnValue(of(userSystemsPage)),
      listUserSystemProfiles: vi.fn().mockReturnValue(of(profileBindings)),
      listProfiles: vi.fn().mockReturnValue(of(profiles)),
      bindUserToSystem: vi.fn().mockReturnValue(of(userSystemsPage.content[0])),
      updateUserSystemStatus: vi.fn().mockReturnValue(of(userSystemsPage.content[0])),
      bindProfileToUserSystem: vi.fn().mockReturnValue(of(profileBindings[0])),
      updateUserSystemProfileStatus: vi.fn().mockReturnValue(of(profileBindings[0])),
    };

    await TestBed.configureTestingModule({
      imports: [UserBindingsComponent],
      providers: [
        provideRouter([]),
        { provide: AdminApiService, useValue: adminApiStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1', userId: '2' }) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserBindingsComponent);
    fixture.detectChanges();
  });

  it('deve carregar sistemas disponíveis, vínculos e perfis vinculados', () => {
    expect(adminApiStub.listSystemsByTenant).toHaveBeenCalledWith('1', 0, 100);
    expect(adminApiStub.listUserSystems).toHaveBeenCalledWith('1', '2', 0, 100);
    expect(fixture.componentInstance.bindings()).toEqual(userSystemsPage.content);
    expect(fixture.componentInstance.profileBindingsByUserSystem['10']).toEqual(profileBindings);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('deve resolver o nome do sistema a partir do id', () => {
    expect(fixture.componentInstance.systemName('3')).toBe('CRM (CRM_ACME)');
    expect(fixture.componentInstance.systemName('99')).toBe('Sistema 99');
  });

  it('deve chamar bindUserToSystem com o systemId selecionado', () => {
    fixture.componentInstance.newSystemId = '3';
    fixture.componentInstance.bindToSystem();
    expect(adminApiStub.bindUserToSystem).toHaveBeenCalledWith('1', '2', { systemId: '3' });
  });

  it('não deve chamar bindUserToSystem sem sistema selecionado', () => {
    fixture.componentInstance.newSystemId = null;
    fixture.componentInstance.bindToSystem();
    expect(adminApiStub.bindUserToSystem).not.toHaveBeenCalled();
  });

  it('deve chamar bindProfileToUserSystem com o profileId selecionado', () => {
    fixture.componentInstance.newProfileId['10'] = '5';
    fixture.componentInstance.bindProfile(userSystemsPage.content[0]);
    expect(adminApiStub.bindProfileToUserSystem).toHaveBeenCalledWith('1', '10', { profileId: '5' });
  });

  it('deve marcar erro quando a listagem inicial falha', () => {
    adminApiStub.listUserSystems.mockReturnValue(throwError(() => new Error('falhou')));
    fixture.componentInstance.load();
    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });
});
