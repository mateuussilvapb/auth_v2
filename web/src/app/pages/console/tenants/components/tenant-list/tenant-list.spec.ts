import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { TenantList } from './tenant-list';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { Page, TenantResponse } from '../../../../../core/models/admin-api.models';

describe('TenantList', () => {
  let fixture: ComponentFixture<TenantList>;
  let adminApiStub: { listTenants: Mock; updateTenantStatus: Mock };
  let router: Router;

  const activeTenant: TenantResponse = { id: '1', code: 'acme', name: 'Acme', status: 'ACTIVE', logoUrl: null };
  const inactiveTenant: TenantResponse = { id: '2', code: 'globex', name: 'Globex', status: 'INACTIVE', logoUrl: null };

  function page(content: TenantResponse[]): Page<TenantResponse> {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 10 };
  }

  beforeEach(async () => {
    adminApiStub = {
      listTenants: vi.fn().mockReturnValue(of(page([activeTenant]))),
      updateTenantStatus: vi.fn().mockReturnValue(of(activeTenant)),
    };

    await TestBed.configureTestingModule({
      imports: [TenantList],
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

    fixture = TestBed.createComponent(TenantList);
    fixture.detectChanges();
  });

  it('deve carregar a lista de tenants ao iniciar', () => {
    expect(fixture.componentInstance.tenants()).toEqual([activeTenant]);
    expect(fixture.componentInstance.totalRecords()).toBe(1);
    expect(fixture.componentInstance.loading()).toBe(false);
    expect(adminApiStub.listTenants).toHaveBeenCalledWith(0, 10);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listTenants.mockReturnValue(throwError(() => new Error('falhou')));

    fixture.componentInstance.load(0);

    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('deve pedir confirmação antes de desativar um tenant ativo', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.toggleStatus(activeTenant);

    expect(confirmSpy).toHaveBeenCalled();
    expect(adminApiStub.updateTenantStatus).not.toHaveBeenCalled();

    confirmSpy.mock.calls[0][0].accept?.();

    expect(adminApiStub.updateTenantStatus).toHaveBeenCalledWith('1', { status: 'INACTIVE' });
  });

  it('deve ativar um tenant inativo sem pedir confirmação', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.toggleStatus(inactiveTenant);

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(adminApiStub.updateTenantStatus).toHaveBeenCalledWith('2', { status: 'ACTIVE' });
  });

  it('deve navegar para a tela de criação', () => {
    fixture.componentInstance.goToCreate();
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants/novo']);
  });

  it('deve navegar para a edição de um tenant', () => {
    fixture.componentInstance.goToEdit(activeTenant);
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants', '1', 'editar']);
  });
});
