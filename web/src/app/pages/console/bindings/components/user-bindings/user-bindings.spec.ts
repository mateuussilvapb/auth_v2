import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { UserBindings } from './user-bindings';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import {
  Page,
  SystemProfileResponse,
  SystemResponse,
  UserSystemProfileResponse,
  UserSystemResponse,
} from '../../../../../core/models/admin-api.models';

describe('UserBindings', () => {
  let fixture: ComponentFixture<UserBindings>;
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

  const activeBinding: UserSystemResponse = { id: '10', userId: '2', systemId: '3', tenantId: '1', status: 'ACTIVE' };
  const userSystemsPage: Page<UserSystemResponse> = {
    content: [activeBinding],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 100,
  };

  const activeProfileBinding: UserSystemProfileResponse = { id: '20', userSystemId: '10', systemProfileId: '5', status: 'ACTIVE' };
  const profileBindings: UserSystemProfileResponse[] = [activeProfileBinding];
  const profiles: SystemProfileResponse[] = [{ id: '5', systemId: '3', code: 'ADMIN', description: 'Administrador', status: 'ACTIVE' }];

  beforeEach(async () => {
    adminApiStub = {
      listSystemsByTenant: vi.fn().mockReturnValue(of(systemsPage)),
      listUserSystems: vi.fn().mockReturnValue(of(userSystemsPage)),
      listUserSystemProfiles: vi.fn().mockReturnValue(of(profileBindings)),
      listProfiles: vi.fn().mockReturnValue(of(profiles)),
      bindUserToSystem: vi.fn().mockReturnValue(of(activeBinding)),
      updateUserSystemStatus: vi.fn().mockReturnValue(of(activeBinding)),
      bindProfileToUserSystem: vi.fn().mockReturnValue(of(activeProfileBinding)),
      updateUserSystemProfileStatus: vi.fn().mockReturnValue(of(activeProfileBinding)),
    };

    await TestBed.configureTestingModule({
      imports: [UserBindings],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1', userId: '2' }) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserBindings);
    fixture.detectChanges();
  });

  it('deve carregar sistemas disponíveis, vínculos e perfis vinculados', () => {
    expect(adminApiStub.listSystemsByTenant).toHaveBeenCalledWith('1', 0, 100);
    expect(adminApiStub.listUserSystems).toHaveBeenCalledWith('1', '2', 0, 100);
    expect(fixture.componentInstance.bindings()).toEqual([activeBinding]);
    expect(fixture.componentInstance.profileBindingsByUserSystem['10']).toEqual(profileBindings);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('deve resolver o nome do sistema a partir do id', () => {
    expect(fixture.componentInstance.systemName('3')).toBe('CRM (CRM_ACME)');
    expect(fixture.componentInstance.systemName('99')).toBe('Sistema 99');
  });

  it('não oferece de novo um sistema já vinculado na lista de vincular', () => {
    expect(fixture.componentInstance.availableSystemsToBind()).toEqual([]);
  });

  it('não oferece de novo um perfil já vinculado na lista de vincular perfil', () => {
    expect(fixture.componentInstance.availableProfilesToBind(activeBinding)).toEqual([]);
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
    fixture.componentInstance.bindProfile(activeBinding);
    expect(adminApiStub.bindProfileToUserSystem).toHaveBeenCalledWith('1', '10', { profileId: '5' });
  });

  it('deve marcar erro quando a listagem inicial falha', () => {
    adminApiStub.listUserSystems.mockReturnValue(throwError(() => new Error('falhou')));
    fixture.componentInstance.load();
    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('vínculo de sistema ativo: bloquear pede confirmação nomeando o sistema', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');
    const action = fixture.componentInstance.systemStatusActions(activeBinding)[0];

    fixture.componentInstance.changeSystemBindingStatus(activeBinding, action);

    expect(confirmSpy).toHaveBeenCalled();
    expect(confirmSpy.mock.calls[0][0].message).toContain('CRM (CRM_ACME)');
    expect(adminApiStub.updateUserSystemStatus).not.toHaveBeenCalled();

    confirmSpy.mock.calls[0][0].accept?.();

    expect(adminApiStub.updateUserSystemStatus).toHaveBeenCalledWith('1', '10', { status: 'BLOCKED' });
  });

  it('vínculo de sistema bloqueado: desbloquear aplica direto, sem confirmação', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');
    const blockedBinding: UserSystemResponse = { ...activeBinding, status: 'BLOCKED' };
    const action = fixture.componentInstance.systemStatusActions(blockedBinding)[0];

    fixture.componentInstance.changeSystemBindingStatus(blockedBinding, action);

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(adminApiStub.updateUserSystemStatus).toHaveBeenCalledWith('1', '10', { status: 'ACTIVE' });
  });

  it('vínculo de perfil ativo: desativar pede confirmação nomeando o perfil', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');
    const action = fixture.componentInstance.profileStatusActions(activeProfileBinding)[1];

    fixture.componentInstance.changeProfileBindingStatus(activeBinding, activeProfileBinding, action);

    expect(confirmSpy).toHaveBeenCalled();
    expect(confirmSpy.mock.calls[0][0].message).toContain('ADMIN');
    confirmSpy.mock.calls[0][0].accept?.();

    expect(adminApiStub.updateUserSystemProfileStatus).toHaveBeenCalledWith('1', '10', '20', { status: 'INACTIVE' });
  });

  it('expandir e recolher uma linha atualiza expandedRowKeys', () => {
    fixture.componentInstance.onRowExpand({ originalEvent: new Event('click'), data: activeBinding });
    expect(fixture.componentInstance.expandedRowKeys()['10']).toBe(true);

    fixture.componentInstance.onRowCollapse({ originalEvent: new Event('click'), data: activeBinding });
    expect(fixture.componentInstance.expandedRowKeys()['10']).toBeUndefined();
  });
});
