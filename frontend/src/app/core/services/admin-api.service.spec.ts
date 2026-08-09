import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AdminApiService } from './admin-api.service';
import { ConsoleAuthService } from './console-auth.service';

describe('AdminApiService', () => {
  let service: AdminApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: ConsoleAuthService, useValue: { getAccessToken: () => 'fake-token' } }],
    });

    service = TestBed.inject(AdminApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve incluir o bearer token do console em toda chamada', () => {
    service.listTenants(0, 20).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants'));
    expect(req.request.headers.get('Authorization')).toBe('Bearer fake-token');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('deve chamar PATCH .../status ao alterar status de tenant', () => {
    service.updateTenantStatus(1, { status: 'INACTIVE' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants/1/status'));
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 1, code: 'acme', name: 'Acme', status: 'INACTIVE', logoUrl: null });
  });

  it('deve criar sistema aninhado sob o tenant', () => {
    service
      .createSystem(1, {
        clientId: 'CRM_ACME',
        name: 'CRM',
        publicClient: true,
        clientSecret: null,
        initialRedirectUris: ['https://crm.acme.com/callback'],
        thirdParty: false,
      })
      .subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants/1/systems'));
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 1,
      clientId: 'CRM_ACME',
      name: 'CRM',
      status: 'ACTIVE',
      publicClient: true,
      redirectUris: ['https://crm.acme.com/callback'],
      thirdParty: false,
    });
  });

  it('deve adicionar redirect URI a um sistema existente', () => {
    service.addRedirectUri(1, { uri: 'https://crm.acme.com/dev-callback' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/redirect-uris'));
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 1,
      clientId: 'CRM_ACME',
      name: 'CRM',
      status: 'ACTIVE',
      publicClient: true,
      redirectUris: ['https://crm.acme.com/callback', 'https://crm.acme.com/dev-callback'],
      thirdParty: false,
    });
  });

  it('deve listar perfis de um sistema sem paginação', () => {
    service.listProfiles(1).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/profiles'));
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, systemId: 1, code: 'ADMIN', description: 'Administrador', status: 'ACTIVE' }]);
  });

  it('deve criar perfil aninhado sob o sistema', () => {
    service.createProfile(1, { code: 'ADMIN', description: 'Administrador' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/profiles'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, systemId: 1, code: 'ADMIN', description: 'Administrador', status: 'ACTIVE' });
  });

  it('deve chamar PATCH .../status ao alterar status de perfil', () => {
    service.updateProfileStatus(1, 2, { status: 'INACTIVE' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/profiles/2/status'));
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 2, systemId: 1, code: 'ADMIN', description: 'Administrador', status: 'INACTIVE' });
  });
});
