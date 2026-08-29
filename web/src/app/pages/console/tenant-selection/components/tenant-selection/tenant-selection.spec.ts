import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { TenantSelection } from './tenant-selection';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { ConsoleAuthService } from '../../../../../core/services/console-auth.service';
import { TenantContextService } from '../../../../../core/services/tenant-context.service';
import { Page, TenantResponse } from '../../../../../core/models/admin-api.models';

function page(content: TenantResponse[]): Page<TenantResponse> {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 10 };
}

describe('TenantSelection', () => {
  let fixture: ComponentFixture<TenantSelection>;
  let adminApi: { listTenants: ReturnType<typeof vi.fn> };
  let tenantContext: { select: ReturnType<typeof vi.fn> };
  let router: Router;

  const tenant: TenantResponse = { id: 't1', code: 'acme', name: 'Acme', status: 'ACTIVE', logoUrl: null };

  beforeEach(async () => {
    adminApi = { listTenants: vi.fn().mockReturnValue(of(page([tenant]))) };
    tenantContext = { select: vi.fn().mockResolvedValue(undefined) };

    await TestBed.configureTestingModule({
      imports: [TenantSelection],
      providers: [
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApi },
        { provide: ConsoleAuthService, useValue: { getAccessToken: () => 'token-abc' } },
        { provide: TenantContextService, useValue: tenantContext },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture = TestBed.createComponent(TenantSelection);
  });

  it('carrega e lista os tenants', () => {
    fixture.detectChanges();

    expect(adminApi.listTenants).toHaveBeenCalledWith(0, 10);
    expect(fixture.componentInstance.tenants()).toEqual([tenant]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('exibe erro quando a listagem falha', () => {
    adminApi.listTenants.mockReturnValue(throwError(() => new Error('falhou')));

    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('ao selecionar, grava o contexto e navega para o dashboard sem returnUrl', async () => {
    fixture.detectChanges();

    await fixture.componentInstance.select(tenant);

    expect(tenantContext.select).toHaveBeenCalledWith({ id: 't1', code: 'acme' }, 'token-abc');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/console');
  });

  it('ao selecionar com returnUrl, navega para a rota original', async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [TenantSelection],
      providers: [
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApi },
        { provide: ConsoleAuthService, useValue: { getAccessToken: () => 'token-abc' } },
        { provide: TenantContextService, useValue: tenantContext },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ returnUrl: '/console/tenants' }) } },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(TenantSelection);
    fixture.detectChanges();

    await fixture.componentInstance.select(tenant);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/console/tenants');
  });
});
