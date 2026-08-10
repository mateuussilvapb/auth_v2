import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import type { Mock } from 'vitest';

import { TenantFormComponent } from './tenant-form.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { TenantResponse } from '../../../core/models/admin-api.models';

describe('TenantFormComponent (criação)', () => {
  let fixture: ComponentFixture<TenantFormComponent>;
  let adminApiStub: { createTenant: Mock; getTenant: Mock };

  beforeEach(async () => {
    adminApiStub = {
      createTenant: vi.fn(),
      getTenant: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [TenantFormComponent],
      providers: [
        provideRouter([{ path: '**', component: TenantFormComponent }]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TenantFormComponent);
    fixture.detectChanges();
  });

  it('não deve carregar tenant nenhum (sem :id na rota)', () => {
    expect(fixture.componentInstance.isEditing).toBe(false);
    expect(adminApiStub.getTenant).not.toHaveBeenCalled();
  });

  it('deve chamar createTenant e navegar para a lista ao salvar', async () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    adminApiStub.createTenant.mockReturnValue(of({ id: '1' } as TenantResponse));

    fixture.componentInstance.code = 'acme';
    fixture.componentInstance.name = 'Acme';
    fixture.componentInstance.submit();

    expect(adminApiStub.createTenant).toHaveBeenCalledWith({ code: 'acme', name: 'Acme' });
    expect(navigateSpy).toHaveBeenCalledWith(['/console/tenants']);
    await fixture.whenStable();
  });

  it('deve mostrar erro quando a criação falha', () => {
    adminApiStub.createTenant.mockReturnValue(
      throwError(() => ({ error: { message: 'código já existe' } })),
    );

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorMessage()).toBe('código já existe');
  });
});

describe('TenantFormComponent (edição)', () => {
  let fixture: ComponentFixture<TenantFormComponent>;
  let adminApiStub: { updateTenant: Mock; getTenant: Mock };

  beforeEach(async () => {
    adminApiStub = {
      updateTenant: vi.fn(),
      getTenant: vi
        .fn()
        .mockReturnValue(of({ id: '1', code: 'acme', name: 'Acme', status: 'ACTIVE', logoUrl: null } as TenantResponse)),
    };

    await TestBed.configureTestingModule({
      imports: [TenantFormComponent],
      providers: [
        provideRouter([{ path: '**', component: TenantFormComponent }]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TenantFormComponent);
    fixture.detectChanges();
  });

  it('deve carregar o tenant existente e preencher o formulário', () => {
    expect(fixture.componentInstance.isEditing).toBe(true);
    expect(fixture.componentInstance.code).toBe('acme');
    expect(fixture.componentInstance.name).toBe('Acme');
  });

  it('deve chamar updateTenant (sem code) ao salvar', () => {
    adminApiStub.updateTenant.mockReturnValue(of({ id: '1' } as TenantResponse));

    fixture.componentInstance.name = 'Acme Corp';
    fixture.componentInstance.submit();

    expect(adminApiStub.updateTenant).toHaveBeenCalledWith('1', { name: 'Acme Corp' });
  });
});
