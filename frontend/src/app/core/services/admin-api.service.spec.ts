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
});
