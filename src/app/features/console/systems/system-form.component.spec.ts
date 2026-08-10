import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import type { Mock } from 'vitest';

import { SystemFormComponent } from './system-form.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { SystemResponse } from '../../../core/models/admin-api.models';

describe('SystemFormComponent', () => {
  let fixture: ComponentFixture<SystemFormComponent>;
  let adminApiStub: { createSystem: Mock };

  beforeEach(async () => {
    adminApiStub = { createSystem: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [SystemFormComponent],
      providers: [
        provideRouter([{ path: '**', component: SystemFormComponent }]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SystemFormComponent);
    fixture.detectChanges();
  });

  it('deve começar com um campo de redirect URI e permitir adicionar/remover', () => {
    expect(fixture.componentInstance.redirectUris.length).toBe(1);

    fixture.componentInstance.addRedirectUriField();
    expect(fixture.componentInstance.redirectUris.length).toBe(2);

    fixture.componentInstance.removeRedirectUriField(0);
    expect(fixture.componentInstance.redirectUris.length).toBe(1);
  });

  it('deve enviar clientSecret null quando publicClient=true', () => {
    adminApiStub.createSystem.mockReturnValue(of({ id: '1' } as SystemResponse));
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.clientId = 'CRM_ACME';
    fixture.componentInstance.name = 'CRM';
    fixture.componentInstance.publicClient = true;
    fixture.componentInstance.redirectUris = ['https://crm.acme.com/callback'];
    fixture.componentInstance.submit();

    expect(adminApiStub.createSystem).toHaveBeenCalledWith('1', {
      clientId: 'CRM_ACME',
      name: 'CRM',
      publicClient: true,
      clientSecret: null,
      initialRedirectUris: ['https://crm.acme.com/callback'],
      thirdParty: false,
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/console/tenants', '1', 'systems']);
  });

  it('deve enviar o clientSecret informado quando publicClient=false', () => {
    adminApiStub.createSystem.mockReturnValue(of({ id: '1' } as SystemResponse));

    fixture.componentInstance.clientId = 'BACKOFFICE_ACME';
    fixture.componentInstance.name = 'Backoffice';
    fixture.componentInstance.publicClient = false;
    fixture.componentInstance.clientSecret = 'super-secreto';
    fixture.componentInstance.redirectUris = ['https://backoffice.acme.com/callback'];
    fixture.componentInstance.submit();

    expect(adminApiStub.createSystem).toHaveBeenCalledWith(
      '1',
      expect.objectContaining({ publicClient: false, clientSecret: 'super-secreto' }),
    );
  });

  it('deve mostrar erro quando a criação falha', () => {
    adminApiStub.createSystem.mockReturnValue(throwError(() => ({ error: { message: 'client_id já existe' } })));

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorMessage()).toBe('client_id já existe');
  });
});
