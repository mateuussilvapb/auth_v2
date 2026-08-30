import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { PlatformAdminList } from './platform-admin-list';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { Page, PlatformAdminResponse } from '../../../../../core/models/admin-api.models';

describe('PlatformAdminList', () => {
  let fixture: ComponentFixture<PlatformAdminList>;
  let adminApiStub: { listPlatformAdmins: Mock; updatePlatformAdminStatus: Mock };
  let router: Router;

  const activeAdmin: PlatformAdminResponse = { id: '1', username: 'root_admin', email: 'root@example.com', name: 'Root Admin', status: 'ACTIVE' };
  const inactiveAdmin: PlatformAdminResponse = { ...activeAdmin, id: '2', username: 'outro_admin', status: 'INACTIVE' };

  function page(content: PlatformAdminResponse[]): Page<PlatformAdminResponse> {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 10 };
  }

  beforeEach(async () => {
    adminApiStub = {
      listPlatformAdmins: vi.fn().mockReturnValue(of(page([activeAdmin]))),
      updatePlatformAdminStatus: vi.fn().mockReturnValue(of(activeAdmin)),
    };

    await TestBed.configureTestingModule({
      imports: [PlatformAdminList],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(PlatformAdminList);
    fixture.detectChanges();
  });

  it('deve carregar a lista de platform admins ao iniciar', () => {
    expect(adminApiStub.listPlatformAdmins).toHaveBeenCalledWith(0, 10);
    expect(fixture.componentInstance.admins()).toEqual([activeAdmin]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listPlatformAdmins.mockReturnValue(throwError(() => new Error('falhou')));
    fixture.componentInstance.load(0);
    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('deve pedir confirmação antes de desativar um platform admin ativo', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.toggleStatus(activeAdmin);

    expect(confirmSpy).toHaveBeenCalled();
    expect(confirmSpy.mock.calls[0][0].message).toContain('root_admin');
    expect(adminApiStub.updatePlatformAdminStatus).not.toHaveBeenCalled();

    confirmSpy.mock.calls[0][0].accept?.();

    expect(adminApiStub.updatePlatformAdminStatus).toHaveBeenCalledWith('1', { status: 'INACTIVE' });
  });

  it('deve ativar um platform admin inativo sem pedir confirmação', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.toggleStatus(inactiveAdmin);

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(adminApiStub.updatePlatformAdminStatus).toHaveBeenCalledWith('2', { status: 'ACTIVE' });
  });

  it('deve navegar para a criação', () => {
    fixture.componentInstance.goToCreate();
    expect(router.navigate).toHaveBeenCalledWith(['/console/platform-admins/novo']);
  });
});
