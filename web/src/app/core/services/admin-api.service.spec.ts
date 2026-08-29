import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AdminApiService } from './admin-api.service';
import { ConsoleAuthService } from './console-auth.service';

describe('AdminApiService', () => {
  let service: AdminApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ConsoleAuthService, useValue: { getAccessToken: () => 'fake-token' } },
      ],
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
    service.updateTenantStatus('1', { status: 'INACTIVE' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants/1/status'));
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: '1', code: 'acme', name: 'Acme', status: 'INACTIVE', logoUrl: null });
  });

  it('deve criar sistema aninhado sob o tenant', () => {
    service
      .createSystem('1', {
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
      id: '1',
      clientId: 'CRM_ACME',
      name: 'CRM',
      status: 'ACTIVE',
      publicClient: true,
      redirectUris: ['https://crm.acme.com/callback'],
      thirdParty: false,
    });
  });

  it('deve adicionar redirect URI a um sistema existente', () => {
    service.addRedirectUri('1', { uri: 'https://crm.acme.com/dev-callback' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/redirect-uris'));
    expect(req.request.method).toBe('POST');
    req.flush({
      id: '1',
      clientId: 'CRM_ACME',
      name: 'CRM',
      status: 'ACTIVE',
      publicClient: true,
      redirectUris: ['https://crm.acme.com/callback', 'https://crm.acme.com/dev-callback'],
      thirdParty: false,
    });
  });

  it('deve remover redirect URI de um sistema existente via query param', () => {
    service.removeRedirectUri('1', 'https://crm.acme.com/dev-callback').subscribe();

    const req = httpMock.expectOne(
      (r) => r.url.endsWith('/admin/api/v1/systems/1/redirect-uris') && r.method === 'DELETE',
    );
    expect(req.request.params.get('uri')).toBe('https://crm.acme.com/dev-callback');
    req.flush({
      id: '1',
      clientId: 'CRM_ACME',
      name: 'CRM',
      status: 'ACTIVE',
      publicClient: true,
      redirectUris: ['https://crm.acme.com/callback'],
      thirdParty: false,
    });
  });

  it('deve rotacionar o secret de um sistema', () => {
    service.rotateSecret('1', { newSecret: 'novo-secret-forte' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/rotate-secret'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ newSecret: 'novo-secret-forte' });
    req.flush({
      id: '1',
      clientId: 'CRM_ACME',
      name: 'CRM',
      status: 'ACTIVE',
      publicClient: false,
      redirectUris: ['https://crm.acme.com/callback'],
      thirdParty: false,
    });
  });

  it('deve listar perfis de um sistema sem paginação', () => {
    service.listProfiles('1').subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/profiles'));
    expect(req.request.method).toBe('GET');
    req.flush([{ id: '1', systemId: '1', code: 'ADMIN', description: 'Administrador', status: 'ACTIVE' }]);
  });

  it('deve criar perfil aninhado sob o sistema', () => {
    service.createProfile('1', { code: 'ADMIN', description: 'Administrador' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/profiles'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: '1', systemId: '1', code: 'ADMIN', description: 'Administrador', status: 'ACTIVE' });
  });

  it('deve chamar PATCH .../status ao alterar status de perfil', () => {
    service.updateProfileStatus('1', '2', { status: 'INACTIVE' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/systems/1/profiles/2/status'));
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: '2', systemId: '1', code: 'ADMIN', description: 'Administrador', status: 'INACTIVE' });
  });

  it('deve criar usuário aninhado sob o tenant', () => {
    service
      .createUser('1', { username: 'joao_silva', email: 'joao@acme.com', password: 'senhaSegura123', name: 'João' })
      .subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants/1/users'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: '1', tenantId: '1', username: 'joao_silva', email: 'joao@acme.com', name: 'João', status: 'ACTIVE' });
  });

  it('deve chamar POST .../reset-password sem corpo', () => {
    service.resetUserPassword('1', '2').subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants/1/users/2/reset-password'));
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('deve vincular usuário a sistema aninhado sob o tenant', () => {
    service.bindUserToSystem('1', '2', { systemId: '3' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants/1/users/2/systems'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: '10', userId: '2', systemId: '3', tenantId: '1', status: 'ACTIVE' });
  });

  it('deve chamar PATCH .../user-systems/:id/status', () => {
    service.updateUserSystemStatus('1', '10', { status: 'BLOCKED' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants/1/user-systems/10/status'));
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: '10', userId: '2', systemId: '3', tenantId: '1', status: 'BLOCKED' });
  });

  it('deve vincular perfil a um vínculo usuário-sistema existente', () => {
    service.bindProfileToUserSystem('1', '10', { profileId: '5' }).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/admin/api/v1/tenants/1/user-systems/10/profiles'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: '20', userSystemId: '10', systemProfileId: '5', status: 'ACTIVE' });
  });
});
