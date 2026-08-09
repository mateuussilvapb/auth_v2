import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SystemListComponent } from './system-list.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { Page, SystemResponse } from '../../../core/models/admin-api.models';

describe('SystemListComponent', () => {
  let fixture: ComponentFixture<SystemListComponent>;
  let adminApiStub: { listSystemsByTenant: jasmine.Spy; updateSystemStatus: jasmine.Spy; addRedirectUri: jasmine.Spy };

  const page: Page<SystemResponse> = {
    content: [
      {
        id: 1,
        clientId: 'CRM_ACME',
        name: 'CRM',
        status: 'ACTIVE',
        publicClient: true,
        redirectUris: ['https://crm.acme.com/callback'],
        thirdParty: false,
      },
    ],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 50,
  };

  beforeEach(async () => {
    adminApiStub = {
      listSystemsByTenant: jasmine.createSpy('listSystemsByTenant').and.returnValue(of(page)),
      updateSystemStatus: jasmine.createSpy('updateSystemStatus').and.returnValue(of(page.content[0])),
      addRedirectUri: jasmine.createSpy('addRedirectUri').and.returnValue(of(page.content[0])),
    };

    await TestBed.configureTestingModule({
      imports: [SystemListComponent],
      providers: [
        provideRouter([]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SystemListComponent);
    fixture.detectChanges();
  });

  it('deve carregar sistemas do tenant da rota', () => {
    expect(adminApiStub.listSystemsByTenant).toHaveBeenCalledWith(1, 0, 50);
    expect(fixture.componentInstance.systems()).toEqual(page.content);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listSystemsByTenant.and.returnValue(throwError(() => new Error('falhou')));
    fixture.componentInstance.load();
    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('deve chamar addRedirectUri com a URI digitada e limpar o campo', () => {
    fixture.componentInstance.newRedirectUri[1] = 'https://crm.acme.com/dev-callback';
    fixture.componentInstance.addRedirectUri(page.content[0]);

    expect(adminApiStub.addRedirectUri).toHaveBeenCalledWith(1, { uri: 'https://crm.acme.com/dev-callback' });
  });

  it('não deve chamar addRedirectUri quando o campo está vazio', () => {
    fixture.componentInstance.newRedirectUri[1] = '';
    fixture.componentInstance.addRedirectUri(page.content[0]);

    expect(adminApiStub.addRedirectUri).not.toHaveBeenCalled();
  });
});
