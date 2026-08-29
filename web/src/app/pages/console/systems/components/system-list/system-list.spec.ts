import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { SystemList } from './system-list';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { Page, SystemResponse } from '../../../../../core/models/admin-api.models';

describe('SystemList', () => {
  let fixture: ComponentFixture<SystemList>;
  let adminApiStub: { listSystemsByTenant: Mock; updateSystemStatus: Mock };
  let dialogServiceStub: { open: Mock };
  let router: Router;

  const activeSystem: SystemResponse = {
    id: '1',
    clientId: 'CRM_ACME',
    name: 'CRM',
    status: 'ACTIVE',
    publicClient: true,
    redirectUris: ['https://crm.acme.com/callback'],
    thirdParty: false,
  };
  const inactiveSystem: SystemResponse = { ...activeSystem, id: '2', clientId: 'ERP_ACME', status: 'INACTIVE' };

  function page(content: SystemResponse[]): Page<SystemResponse> {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 10 };
  }

  beforeEach(async () => {
    adminApiStub = {
      listSystemsByTenant: vi.fn().mockReturnValue(of(page([activeSystem]))),
      updateSystemStatus: vi.fn().mockReturnValue(of(activeSystem)),
    };
    dialogServiceStub = { open: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [SystemList],
      providers: [
        provideRouter([]),
        ConfirmationService,
        MessageServicePG,
        { provide: DialogService, useValue: dialogServiceStub },
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: 't1' }) } } },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(SystemList);
    fixture.detectChanges();
  });

  it('deve carregar os sistemas do tenant da rota', () => {
    expect(fixture.componentInstance.tenantId).toBe('t1');
    expect(adminApiStub.listSystemsByTenant).toHaveBeenCalledWith('t1', 0, 10);
    expect(fixture.componentInstance.systems()).toEqual([activeSystem]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listSystemsByTenant.mockReturnValue(throwError(() => new Error('falhou')));

    fixture.componentInstance.load(0);

    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('deve pedir confirmação antes de desativar um sistema ativo', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.toggleStatus(activeSystem);

    expect(confirmSpy).toHaveBeenCalled();
    expect(adminApiStub.updateSystemStatus).not.toHaveBeenCalled();

    confirmSpy.mock.calls[0][0].accept?.();

    expect(adminApiStub.updateSystemStatus).toHaveBeenCalledWith('1', { status: 'INACTIVE' });
  });

  it('deve ativar um sistema inativo sem pedir confirmação', () => {
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.toggleStatus(inactiveSystem);

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(adminApiStub.updateSystemStatus).toHaveBeenCalledWith('2', { status: 'ACTIVE' });
  });

  it('deve abrir o diálogo de edição com os dados do sistema e recarregar ao fechar com resultado', () => {
    const onClose = new Subject<boolean | undefined>();
    dialogServiceStub.open.mockReturnValue({ onClose });

    fixture.componentInstance.edit(activeSystem);

    expect(dialogServiceStub.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ data: { mode: 'edicao', id: '1', valoresIniciais: activeSystem } }),
    );

    adminApiStub.listSystemsByTenant.mockClear();
    onClose.next(true);

    expect(adminApiStub.listSystemsByTenant).toHaveBeenCalled();
  });

  it('não recarrega quando o diálogo fecha sem resultado (cancelado)', () => {
    const onClose = new Subject<boolean | undefined>();
    dialogServiceStub.open.mockReturnValue({ onClose });

    fixture.componentInstance.edit(activeSystem);
    adminApiStub.listSystemsByTenant.mockClear();
    onClose.next(undefined);

    expect(adminApiStub.listSystemsByTenant).not.toHaveBeenCalled();
  });

  it('deve navegar para a criação de sistema com o tenantId', () => {
    fixture.componentInstance.goToCreate();
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants', 't1', 'systems', 'novo']);
  });

  it('deve navegar para os perfis do sistema', () => {
    fixture.componentInstance.goToProfiles(activeSystem);
    expect(router.navigate).toHaveBeenCalledWith(['/console/systems', '1', 'profiles']);
  });
});
