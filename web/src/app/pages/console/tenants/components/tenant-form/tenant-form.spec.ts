import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { TenantForm } from './tenant-form';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { MessageService } from '../../../../../shared/services/message.service';
import { TenantResponse } from '../../../../../core/models/admin-api.models';

describe('TenantForm', () => {
  let fixture: ComponentFixture<TenantForm>;
  let adminApiStub: { getTenant: Mock; createTenant: Mock; updateTenant: Mock };
  let router: Router;
  let messageService: MessageService;

  const tenant: TenantResponse = { id: '1', code: 'acme', name: 'Acme', status: 'ACTIVE', logoUrl: null };

  async function setup(routeData: Record<string, unknown>, paramMap: Record<string, string> = {}): Promise<void> {
    adminApiStub = {
      getTenant: vi.fn().mockReturnValue(of(tenant)),
      createTenant: vi.fn().mockReturnValue(of(tenant)),
      updateTenant: vi.fn().mockReturnValue(of(tenant)),
    };

    await TestBed.configureTestingModule({
      imports: [TenantForm],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map(Object.entries(paramMap)), data: routeData } },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    messageService = TestBed.inject(MessageService);

    fixture = TestBed.createComponent(TenantForm);
    fixture.detectChanges();
  }

  it('modo criação: form vazio, código habilitado, cria e navega ao submeter', async () => {
    await setup({ formMode: 'cadastro' });

    expect(fixture.componentInstance.isCreateMode()).toBe(true);
    expect(fixture.componentInstance.form.get('code')?.disabled).toBe(false);

    fixture.componentInstance.form.setValue({ code: 'globex', name: 'Globex' });
    await fixture.componentInstance.submit();

    expect(adminApiStub.createTenant).toHaveBeenCalledWith({ code: 'globex', name: 'Globex' });
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants']);
  });

  it('modo edição: carrega o tenant, desabilita código e atualiza só o nome ao submeter', async () => {
    await setup({ formMode: 'edicao' }, { id: '1' });

    expect(fixture.componentInstance.isEditMode()).toBe(true);
    expect(adminApiStub.getTenant).toHaveBeenCalledWith('1');
    expect(fixture.componentInstance.form.get('code')?.disabled).toBe(true);
    expect(fixture.componentInstance.form.getRawValue()).toEqual({ code: 'acme', name: 'Acme' });

    fixture.componentInstance.form.patchValue({ name: 'Acme Corp' });
    await fixture.componentInstance.submit();

    expect(adminApiStub.updateTenant).toHaveBeenCalledWith('1', { name: 'Acme Corp' });
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants']);
  });

  it('deve mostrar erro quando falha ao carregar o tenant em modo edição', async () => {
    adminApiStub = {
      getTenant: vi.fn().mockReturnValue(throwError(() => new Error('falhou'))),
      createTenant: vi.fn(),
      updateTenant: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [TenantForm],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['id', '1']]), data: { formMode: 'edicao' } } },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    messageService = TestBed.inject(MessageService);
    const errorSpy = vi.spyOn(messageService, 'showError');

    fixture = TestBed.createComponent(TenantForm);
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve));

    expect(errorSpy).toHaveBeenCalledWith('Não foi possível carregar o tenant.');
  });

  it('deve mostrar a mensagem de erro da API quando falha ao salvar', async () => {
    await setup({ formMode: 'cadastro' });
    adminApiStub.createTenant.mockReturnValue(
      throwError(() => ({ error: { message: 'Código já em uso.' } })),
    );
    const errorSpy = vi.spyOn(messageService, 'showError');

    fixture.componentInstance.form.setValue({ code: 'acme', name: 'Acme' });
    await fixture.componentInstance.submit();

    expect(errorSpy).toHaveBeenCalledWith('Código já em uso.');
    expect(fixture.componentInstance.submitting()).toBe(false);
  });

  it('não submete quando o formulário é inválido', async () => {
    await setup({ formMode: 'cadastro' });

    await fixture.componentInstance.submit();

    expect(adminApiStub.createTenant).not.toHaveBeenCalled();
  });
});
