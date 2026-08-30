import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { UserList } from './user-list';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { Page, UserResponse } from '../../../../../core/models/admin-api.models';

describe('UserList', () => {
  let fixture: ComponentFixture<UserList>;
  let adminApiStub: { listUsers: Mock; updateUserStatus: Mock; resetUserPassword: Mock };
  let router: Router;

  const activeUser: UserResponse = {
    id: '1',
    tenantId: 't1',
    username: 'joao_silva',
    email: 'joao@acme.com',
    name: 'João Silva',
    status: 'ACTIVE',
  };
  const blockedUser: UserResponse = { ...activeUser, id: '2', username: 'maria', status: 'BLOCKED' };
  const disabledUser: UserResponse = { ...activeUser, id: '3', username: 'pedro', status: 'DISABLED' };

  function page(content: UserResponse[]): Page<UserResponse> {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 10 };
  }

  beforeEach(async () => {
    adminApiStub = {
      listUsers: vi.fn().mockReturnValue(of(page([activeUser]))),
      updateUserStatus: vi.fn().mockReturnValue(of(activeUser)),
      resetUserPassword: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.configureTestingModule({
      imports: [UserList],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: 't1' }) } } },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(UserList);
    fixture.detectChanges();
  });

  it('deve carregar os usuários do tenant da rota', () => {
    expect(fixture.componentInstance.tenantId).toBe('t1');
    expect(adminApiStub.listUsers).toHaveBeenCalledWith('t1', 0, 10);
    expect(fixture.componentInstance.users()).toEqual([activeUser]);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listUsers.mockReturnValue(throwError(() => new Error('falhou')));
    fixture.componentInstance.load(0);
    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('usuário ativo: oferece bloquear e desativar, ambos com confirmação', () => {
    const actions = fixture.componentInstance.statusActionsFor(activeUser);
    expect(actions.map((a) => a.target)).toEqual(['BLOCKED', 'DISABLED']);
    expect(actions.every((a) => a.confirm)).toBe(true);
  });

  it('usuário bloqueado: oferece só desbloquear, sem confirmação', () => {
    const actions = fixture.componentInstance.statusActionsFor(blockedUser);
    expect(actions).toEqual([{ label: 'Desbloquear', target: 'ACTIVE', severity: 'secondary', confirm: false }]);
  });

  it('usuário desativado: oferece só ativar, sem confirmação', () => {
    const actions = fixture.componentInstance.statusActionsFor(disabledUser);
    expect(actions).toEqual([{ label: 'Ativar', target: 'ACTIVE', severity: 'secondary', confirm: false }]);
  });

  it('bloquear pede confirmação nomeando o usuário antes de chamar a API', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');
    const action = fixture.componentInstance.statusActionsFor(activeUser)[0];

    fixture.componentInstance.changeStatus(activeUser, action);

    expect(confirmSpy).toHaveBeenCalled();
    expect(confirmSpy.mock.calls[0][0].message).toContain('joao_silva');
    expect(adminApiStub.updateUserStatus).not.toHaveBeenCalled();

    confirmSpy.mock.calls[0][0].accept?.();

    expect(adminApiStub.updateUserStatus).toHaveBeenCalledWith('t1', '1', { status: 'BLOCKED' });
  });

  it('desbloquear aplica direto, sem confirmação', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');
    const action = fixture.componentInstance.statusActionsFor(blockedUser)[0];

    fixture.componentInstance.changeStatus(blockedUser, action);

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(adminApiStub.updateUserStatus).toHaveBeenCalledWith('t1', '2', { status: 'ACTIVE' });
  });

  it('deve disparar o reset de senha e mostrar o e-mail de destino', () => {
    fixture.componentInstance.resetPassword(activeUser);
    expect(adminApiStub.resetUserPassword).toHaveBeenCalledWith('t1', '1');
  });

  it('deve navegar para a criação de usuário com o tenantId', () => {
    fixture.componentInstance.goToCreate();
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants', 't1', 'users', 'novo']);
  });

  it('deve navegar para os vínculos de um usuário', () => {
    fixture.componentInstance.goToBindings(activeUser);
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants', 't1', 'users', '1', 'bindings']);
  });
});
